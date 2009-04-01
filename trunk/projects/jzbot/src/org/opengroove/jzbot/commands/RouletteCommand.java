package org.opengroove.jzbot.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.opengroove.jzbot.Command;
import org.opengroove.jzbot.JZBot;
import org.opengroove.jzbot.commands.roulette.RouletteState;

public class RouletteCommand implements Command
{
    protected static final long TIME_TO_EXPIRE = 1000 * 60 * 5;
    private static Map<String, RouletteState> stateMap =
        Collections.synchronizedMap(new HashMap<String, RouletteState>());
    
    static
    {
        new Thread()
        {
            public void run()
            {
                while (JZBot.isRunning)
                {
                    try
                    {
                        Thread.sleep(30 * 1000);
                        for (String key : new ArrayList<String>(stateMap.keySet()))
                        {
                            RouletteState value = stateMap.get(key);
                            if (value != null)
                            {
                                if ((value.changed + TIME_TO_EXPIRE) < System
                                    .currentTimeMillis())
                                    stateMap.remove(key);
                            }
                        }
                    }
                    catch (Exception exception)
                    {
                        exception.printStackTrace();
                    }
                }
            }
        }.start();
    }
    
    public String getName()
    {
        return "roulette";
    }
    
    public synchronized void run(String channel, boolean pm, String sender,
        String hostname, String arguments)
    {
        if (channel == null)
        {
            JZBot.bot.sendMessage(sender,
                "You can only use roulette when a channel is specified.");
            return;
        }
        RouletteState state = stateMap.get(channel);
        if (state == null)
        {
            state = new RouletteState();
            state.changed = System.currentTimeMillis();
            state.current = 0;
            state.loaded = (int) ((Math.random() * 6.0) + 1.0);
            stateMap.put(channel, state);
        }
        if (arguments.equals("reset"))
        {
            if (JZBot.isOp(channel, hostname))
            {
                stateMap.remove(channel);
                JZBot.bot.sendMessage(pm ? sender : channel, "Roulette reset.");
            }
            else
            {
                
            }
        }
        state.changed = System.currentTimeMillis();
        state.current++;
        String prefix = "" + sender + ": (Chamber " + state.current + " of 6) ";
        if (state.current == state.loaded)
        {
            stateMap.remove(channel);
            JZBot.bot.sendMessage(channel, prefix + "*BANG* You're dead.");
            JZBot.bot.sendAction(channel, "reloads and spins the chamber");
        }
        else
        {
            JZBot.bot.sendMessage(channel, prefix + "*click*");
        }
    }
}
