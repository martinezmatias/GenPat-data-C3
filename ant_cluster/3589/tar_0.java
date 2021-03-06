/*
 * Copyright  2002-2005 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.tools.ant.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogConfigurationException;
import org.apache.commons.logging.LogFactory;

import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.BuildLogger;
import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.UnknownElement;

import java.io.PrintStream;

/**
 * Jakarta Commons Logging listener.
 * Note: do not use the SimpleLog as your logger implementation as it
 * causes an infinite loop since it writes to System.err, which Ant traps
 * and reroutes to the logger/listener layer.
 *
 * The following names are used for the log:
 *  org.apache.tools.ant.Project.PROJECT_NAME  - for project events
 *  org.apache.tools.ant.Target.TARGET_NAME - for target events
 *  TASK_CLASS_NAME.TARGET_NAME - for events in individual targets.
 *
 * In all target and project names we replace "." and " " with "-".
 *
 * TODO: we should use the advanced context logging features (and expose them
 * in c-l first :-)
 * TODO: this is _very_ inefficient. Switching the out and tracking the logs
 * can be optimized a lot - but may require few more changes to the core.
 *
 * @since Ant 1.5
 */
public class CommonsLoggingListener implements BuildListener, BuildLogger {

    /** Indicates if the listener was initialized. */
    private boolean initialized = false;

    private LogFactory logFactory;

    /**
     * name of the category under which target events are logged
     */
    public static final String TARGET_LOG = "org.apache.tools.ant.Target";
    /**
     * name of the category under which project events are logged
     */
    public static final String PROJECT_LOG = "org.apache.tools.ant.Project";

    /**
     * Construct the listener and make sure that a LogFactory
     * can be obtained.
     */
    public CommonsLoggingListener() {
    }

    private Log getLog(String cat, String suffix) {
        if (suffix != null) {
            suffix = suffix.replace('.', '-');
            suffix = suffix.replace(' ', '-');
            cat = cat + "." + suffix;
        }
        PrintStream tmpOut = System.out;
        PrintStream tmpErr = System.err;
        System.setOut(out);
        System.setErr(err);

        if (!initialized) {
            try {
                logFactory = LogFactory.getFactory();
            } catch (LogConfigurationException e) {
                e.printStackTrace(System.err);
                return null;
            }
        }

        initialized = true;
        Log log = logFactory.getInstance(cat);
        System.setOut(tmpOut);
        System.setErr(tmpErr);
        return log;
    }

    /**
     * @see BuildListener#buildStarted
     */
    public void buildStarted(BuildEvent event) {
        String categoryString = PROJECT_LOG;
        Log log = getLog(categoryString, null);

        if (initialized) {
            realLog(log, "Build started.", Project.MSG_INFO, null);
        }
    }

    /**
     * @see BuildListener#buildFinished
     */
    public void buildFinished(BuildEvent event) {
        if (initialized) {
            String categoryString = PROJECT_LOG;
            Log log = getLog(categoryString, event.getProject().getName());

            if (event.getException() == null) {
                realLog(log, "Build finished.", Project.MSG_INFO, null);
            } else {
                realLog(log, "Build finished with error.", Project.MSG_ERR,
                        event.getException());
            }
        }
    }

    /**
     * @see BuildListener#targetStarted
     */
    public void targetStarted(BuildEvent event) {
        if (initialized) {
            Log log = getLog(TARGET_LOG,
                    event.getTarget().getName());
            // Since task log category includes target, we don't really
            // need this message
            realLog(log, "Start: " + event.getTarget().getName(),
                    Project.MSG_VERBOSE, null);
        }
    }

    /**
     * @see BuildListener#targetFinished
     */
    public void targetFinished(BuildEvent event) {
        if (initialized) {
            String targetName = event.getTarget().getName();
            Log log = getLog(TARGET_LOG,
                    event.getTarget().getName());
            if (event.getException() == null) {
                realLog(log, "Target end: " + targetName, Project.MSG_DEBUG, null);
            } else {
                realLog(log, "Target \"" + targetName
                        + "\" finished with error.", Project.MSG_ERR,
                        event.getException());
            }
        }
    }

    /**
     * @see BuildListener#taskStarted
     */
    public void taskStarted(BuildEvent event) {
        if (initialized) {
            Task task = event.getTask();
            Object real = task;
            if (task instanceof UnknownElement) {
                Object realObj = ((UnknownElement) task).getTask();
                if (realObj != null) {
                    real = realObj;
                }
            }
            Log log = getLog(real.getClass().getName(), null);
            if (log.isTraceEnabled()) {
                realLog(log, "Task \"" + task.getTaskName() + "\" started ",
                        Project.MSG_VERBOSE, null);
            }
        }
    }

    /**
     * @see BuildListener#taskFinished
     */
    public void taskFinished(BuildEvent event) {
        if (initialized) {
            Task task = event.getTask();
            Object real = task;
            if (task instanceof UnknownElement) {
                Object realObj = ((UnknownElement) task).getTask();
                if (realObj != null) {
                    real = realObj;
                }
            }
            Log log = getLog(real.getClass().getName(), null);
            if (event.getException() == null) {
                if (log.isTraceEnabled()) {
                    realLog(log, "Task \"" + task.getTaskName() + "\" finished.",
                            Project.MSG_VERBOSE, null);
                }
            } else {
                realLog(log, "Task \"" + task.getTaskName()
                        + "\" finished with error.", Project.MSG_ERR,
                        event.getException());
            }
        }
    }


    /**
     * @see BuildListener#messageLogged
     */
    public void messageLogged(BuildEvent event) {
        if (initialized) {
            Object categoryObject = event.getTask();
            String categoryString = null;
            String categoryDetail = null;

            if (categoryObject == null) {
                categoryObject = event.getTarget();
                if (categoryObject == null) {
                    categoryObject = event.getProject();
                    categoryString = PROJECT_LOG;
                    categoryDetail = event.getProject().getName();
                } else {
                    categoryString = TARGET_LOG;
                    categoryDetail = event.getTarget().getName();
                }
            } else {
                // It's a task - append the target
                if (event.getTarget() != null) {
                    categoryString = categoryObject.getClass().getName();
                    categoryDetail = event.getTarget().getName();
                } else {
                    categoryString = categoryObject.getClass().getName();
                }

            }

            Log log = getLog(categoryString, categoryDetail);
            int priority = event.getPriority();
            String message = event.getMessage();
            realLog(log, message, priority , null);
        }
    }

    private void realLog(Log log, String message, int priority, Throwable t) {
        PrintStream tmpOut = System.out;
        PrintStream tmpErr = System.err;
        System.setOut(out);
        System.setErr(err);
        switch (priority) {
            case Project.MSG_ERR:
                if (t == null) {
                    log.error(message);
                } else {
                    log.error(message, t);
                }
                break;
            case Project.MSG_WARN:
                if (t == null) {
                    log.warn(message);
                } else {
                    log.warn(message, t);
                }
                break;
            case Project.MSG_INFO:
                if (t == null) {
                    log.info(message);
                } else {
                    log.info(message, t);
                }
                break;
            case Project.MSG_VERBOSE:
                log.debug(message);
                break;
            case Project.MSG_DEBUG:
                log.debug(message);
                break;
            default:
                log.error(message);
                break;
        }
        System.setOut(tmpOut);
        System.setErr(tmpErr);
    }

    PrintStream out = System.out;
    PrintStream err = System.err;

    /**
     * Set the the output level.
     * This is not used, the logger config is used instead.
     * @param level ignored
     */
    public void setMessageOutputLevel(int level) {
        // Use the logger config
    }

    /**
     * Set the output print stream.
     * @param output the output stream
     */
    public void setOutputPrintStream(PrintStream output) {
        this.out = output;
    }

    /**
     * Set emacs mode.
     * This is ignored.
     * @param emacsMode ignored
     */
    public void setEmacsMode(boolean emacsMode) {
        // Doesn't make sense for c-l. Use the logger config
    }

    /**
     * Set the error print stream.
     * @param err the error stream
     */
    public void setErrorPrintStream(PrintStream err) {
        this.err = err;
    }

}
