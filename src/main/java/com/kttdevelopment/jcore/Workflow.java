/*
 * Copyright (C) 2021 Ktt Development
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.kttdevelopment.jcore;

import java.util.*;

public abstract class Workflow {

    // ----- variables ---------------

    public static void exportVariable(final String name, final Object value){
        throw new UnsupportedOperationException("Export variable is not supported");
    }

    public static void addMask(final String mask){
        setSecret(mask);
    }

    public static void setSecret(final String secret){
        issueCommand("add-mask", secret);
    }

    public static void addPath(final String name){
        throw new UnsupportedOperationException("Add path is not supported");
    }

    public static String getInput(final String name){
        return getInput(name, false, true);
    }

    public static String getInput(final String name, final boolean required){
        return getInput(name, required, true);
    }

    public static String getInput(final String name, final boolean required, final boolean trimWhitespace){
        final String input = name != null ? "INPUT_" + name.replace(' ', '_').toUpperCase() : "";
        final String value = System.getenv(input);
        if(required && value == null)
            throw new NullPointerException("Input '" + name + "' is required and not supplied");
        else
            return value != null
                ? trimWhitespace
                    ? value.trim()
                    : value
                : null;
    }

    public static String[] getMultilineInput(final String name){
        return getMultilineInput(name, false, true);
    }

    public static String[] getMultilineInput(final String name, final boolean required){
        return getMultilineInput(name, required, true);
    }

    public static String[] getMultilineInput(final String name, final boolean required, final boolean trimWhitespace){
        final String input = getInput(name, required, trimWhitespace);
        return input == null
            ? new String[0]
            : Arrays.stream(input
                .split("\\n"))
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
    }

    public static boolean getBooleanInput(final String name){
        return getBooleanInput(name, true, true);
    }

    public static boolean getBooleanInput(final String name, final boolean required){
        return getBooleanInput(name, required, true);
    }

    public static boolean getBooleanInput(final String name, final boolean required, final boolean trimWhitespace){
        final String input = getInput(name, required, trimWhitespace);

        if(input != null)
            if(input.equalsIgnoreCase("true"))
                return true;
            else if(input.equalsIgnoreCase("false"))
                return false;
        throw new IllegalArgumentException("Input '" + name + "' is not a boolean type");
    }

    public static void setOutput(final String name, final Object value){
        issueCommand("set-output", new HashMap<String,Object>(){{
            put("name", name);
        }}, value);
    }

    public static void setCommandEcho(final boolean enabled){
        issueCommand("echo", enabled ? "on" : "off");
    }

    // ----- results ---------------

    public static void setFailed(final String error){
        error(error);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> System.exit(1)));
    }

    // ----- logging commands ---------------

    public static boolean isDebug(){
        return System.getenv("RUNNER_DEBUG").equals("1");
    }

   public static void debug(final String debug){
        issueCommand("debug", debug);
    }

    public static void error(final Throwable throwable){
        issueCommand("error", new HashMap<String,Object>(){{
            put("file", throwable.getStackTrace()[0].getFileName());
            put("line", throwable.getStackTrace()[0].getLineNumber());
        }}, throwable.getMessage());
    }

    public static void error(final String error){
        final StackTraceElement trace = new Throwable().getStackTrace()[1];
        issueCommand("error", new HashMap<String,Object>(){{
            put("file", trace.getFileName());
            put("line", trace.getLineNumber());
        }}, error);
    }

    public static void warning(final Throwable throwable){
        issueCommand("warning", new HashMap<String,Object>(){{
            put("file", throwable.getStackTrace()[0].getFileName());
            put("line", throwable.getStackTrace()[0].getLineNumber());
        }}, throwable.getMessage());
    }

    public static void warning(final String warning){
        final StackTraceElement trace = new Throwable().getStackTrace()[1];
        issueCommand("warning", new HashMap<String,Object>(){{
            put("file", trace.getFileName());
            put("line", trace.getLineNumber());
        }}, warning);
    }

    public static void info(final String message){
        System.out.println(message);
    }

    public static void startGroup(final String name){
        issueCommand("group", name);
    }

    public static void startGroup(final String name, final Runnable runnable){
        startGroup(name);
        try{
            runnable.run();
        }finally{
            endGroup();
        }
    }

    @SuppressWarnings("SpellCheckingInspection")
    public static void endGroup(){
        issueCommand("endgroup");
    }

    // ----- state ---------------

    public static void saveState(final String name, final Object value){
        issueCommand("save-state", new HashMap<String,Object>(){{
            put("name", name);
        }}, value);
    }

    public static String getState(final String state){
        return System.getenv("STATE_" + state);
    }

    // ----- commands ---------------

    public static void stopCommand(final String token){
        issueCommand("stop-commands", token);
    }

    public static void startCommand(final String token){
        issueCommand(token);
    }

    // ----- command ---------------

    private static void issueCommand(final String command){
        issueCommand(command, null, null);
    }

    private static void issueCommand(final String command, final String message){
        issueCommand(command, null, message);
    }

    private static void issueCommand(final String command, final Map<String,Object> properties, final Object message){
        System.out.println(toCommand(command, properties, message));
    }

    private static final String commandString = "::";

    private static String toCommand(final String command, final Map<String,Object> properties, final Object message){
        final StringBuilder commandString = new StringBuilder(Workflow.commandString + (command != null ? command : "missing.command"));

        if(properties != null && !properties.isEmpty()){
            commandString.append(' ');
            boolean first = true;
            for(final String key : properties.keySet()){
                final Object value = properties.get(key);
                if(value != null){
                    if(first)
                        first = false;
                    else
                        commandString.append(',');
                    commandString
                        .append(key)
                        .append('=')
                        .append(
                            toCommandValue(value)
                                .replaceAll("%", "%25")
                                .replaceAll("\\r", "%0D")
                                .replaceAll("\\n", "%0A")
                                .replaceAll(":", "%3A")
                                .replaceAll(",", "%2C")
                        );
                }
            }
        }
        commandString.append(Workflow.commandString);
        commandString.append(
            message != null
                ? toCommandValue(message)
                    .replaceAll("%", "%25")
                    .replaceAll("\\r", "%0D")
                    .replaceAll("\\n", "%0A")
                : ""
        );

        return commandString.toString();
    }

    private static String toCommandValue(final Object obj){
        if(obj == null)
            return "";
        else if(obj instanceof String)
            return (String) obj;
        else
            return obj.toString();
        // todo: obj to json string
    }

}
