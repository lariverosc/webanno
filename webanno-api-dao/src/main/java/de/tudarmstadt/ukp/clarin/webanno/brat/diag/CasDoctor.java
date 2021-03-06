/*******************************************************************************
 * Copyright 2015
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.brat.diag;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.uima.cas.CAS;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import de.tudarmstadt.ukp.clarin.webanno.brat.diag.checks.Check;
import de.tudarmstadt.ukp.clarin.webanno.brat.diag.repairs.Repair;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;

public class CasDoctor
    implements InitializingBean, ApplicationContextAware
{
    private Log log = LogFactory.getLog(getClass());

    @Value(value = "${debug.casDoctor.checks}")
    private String activeChecks;

    @Value(value = "${debug.casDoctor.fatal}")
    private boolean fatalChecks = true;

    @Value(value = "${debug.casDoctor.repairs}")
    private String activeRepairs;

    private ApplicationContext context;
    
    private List<Class<? extends Check>> checkClasses = new ArrayList<>();
    private List<Class<? extends Repair>> repairClasses = new ArrayList<>();

    public CasDoctor()
    {
        // Bean operation
    }

    public CasDoctor(Class<?>... aChecksRepairs)
    {
        // For testing
        StringBuilder checks = new StringBuilder();
        StringBuilder repairs = new StringBuilder();
        for (Class<?> clazz : aChecksRepairs) {
            boolean isCheck = Check.class.isAssignableFrom(clazz);
            boolean isRepair = Repair.class.isAssignableFrom(clazz);
            
            if (isCheck) {
                if (checks.length() > 0) {
                    checks.append(',');
                }
                checks.append(clazz.getSimpleName());
            }
            
            if (isRepair) {
                if (repairs.length() > 0) {
                    repairs.append(',');
                }
                repairs.append(clazz.getSimpleName());
            }
            
            if (!isCheck && !isRepair) {
                throw new IllegalArgumentException("[" + clazz.getName()
                        + "] is neither a check nor a repair");
            }
        }
        activeChecks = checks.toString();
        fatalChecks = false;
        
        activeRepairs = repairs.toString();
        
        afterPropertiesSet();
    }

    public void setFatalChecks(boolean aFatalChecks)
    {
        fatalChecks = aFatalChecks;
    }

    public boolean isFatalChecks()
    {
        return fatalChecks;
    }

    public void repair(Project aProject, CAS aCas)
    {
        List<LogMessage> messages = new ArrayList<>();
        repair(aProject, aCas, messages);
        if (log.isWarnEnabled() && !messages.isEmpty()) {
            messages.forEach(s -> log.warn(s));
        }
    }
    
    public void repair(Project aProject, CAS aCas, List<LogMessage> aMessages)
    {
        boolean exception = false;
        for (Class<? extends Repair> repairClass : repairClasses) {
            try {
                Repair repair = repairClass.newInstance();
                if (context != null) {
                    context.getAutowireCapableBeanFactory().autowireBean(repair);
                }
                repair.repair(aProject, aCas, aMessages);
            }
            catch (Exception e) {
                aMessages.add(new LogMessage(this, LogLevel.ERROR, "Cannot perform repair [%s]: %s",
                        repairClass.getSimpleName(), ExceptionUtils.getRootCauseMessage(e)));
                log.error("Error running repair", e);
                exception = true;
            }
        }
        
        if (!repairClasses.isEmpty() && (exception || !analyze(aProject, aCas, aMessages, false))) {
            aMessages.forEach(s -> log.error(s));
            throw new IllegalStateException("Repair attempt failed - ask system administrator "
                    + "for details.");
        }
    }
    
    public boolean analyze(Project aProject, CAS aCas)
    {
        List<LogMessage> messages = new ArrayList<>();
        boolean result = analyze(aProject, aCas, messages);
        if (log.isDebugEnabled()) {
            messages.forEach(s -> log.debug(s));
        }
        return result;
    }

    public boolean analyze(Project aProject, CAS aCas, List<LogMessage> aMessages)
    {
        return analyze(aProject, aCas, aMessages, isFatalChecks());
    }

    private boolean analyze(Project aProject, CAS aCas, List<LogMessage> aMessages,
            boolean aFatalChecks)
    {
        boolean ok = true;
        for (Class<? extends Check> checkClass : checkClasses) {
            try {
                Check check = checkClass.newInstance();
                if (context != null) {
                    context.getAutowireCapableBeanFactory().autowireBean(check);
                }
                ok &= check.check(aProject, aCas, aMessages);
            }
            catch (InstantiationException | IllegalAccessException e) {
                aMessages.add(new LogMessage(this, LogLevel.ERROR, "Cannot instantiate [%s]: %s",
                        checkClass.getSimpleName(), ExceptionUtils.getRootCauseMessage(e)));
                log.error("Error running check", e);
            }
        }

        if (!ok && aFatalChecks) {
            aMessages.forEach(s -> log.error(s));
            throw new IllegalStateException("CasDoctor has detected problems and checks are fatal.");
        }

        return ok;
    }

    public void setActiveChecks(String aActiveChecks)
    {
        activeChecks = aActiveChecks;
    }

    public void setActiveRepairs(String aActiveRepairs)
    {
        activeRepairs = aActiveRepairs;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void afterPropertiesSet()
    {
        if (StringUtils.isNotBlank(activeChecks)) {
            for (String check : activeChecks.split(",")) {
                try {
                    checkClasses.add((Class<? extends Check>) Class.forName(Check.class
                            .getPackage().getName() + "." + check.trim()));
                }
                catch (ClassNotFoundException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        
        if (StringUtils.isNotBlank(activeRepairs)) {
            for (String check : activeRepairs.split(",")) {
                try {
                    repairClasses.add((Class<? extends Repair>) Class.forName(Repair.class
                            .getPackage().getName() + "." + check.trim()));
                }
                catch (ClassNotFoundException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
    }

    public static enum LogLevel
    {
        INFO, ERROR
    }

    public static class LogMessage
    {
        public final LogLevel level;
        public final Class<?> source;
        public final String message;

        public LogMessage(Object aSource, LogLevel aLevel, String aMessage)
        {
            this(aSource, aLevel, "%s", aMessage);
        }

        public LogMessage(Object aSource, LogLevel aLevel, String aFormat, Object... aValues)
        {
            super();
            source = aSource != null ? aSource.getClass() : null;
            level = aLevel;
            message = String.format(aFormat, aValues);
        }
        
        @Override
        public String toString()
        {
            return String.format("[%s] %s", source != null ? source.getSimpleName() : "<unknown>",
                    message);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext aContext)
        throws BeansException
    {
        context = aContext;
    }
}
