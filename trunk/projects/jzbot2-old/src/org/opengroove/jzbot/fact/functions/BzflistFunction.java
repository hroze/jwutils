package org.opengroove.jzbot.fact.functions;

import java.util.ArrayList;
import java.util.Map;

import net.sf.opengroove.common.utils.StringUtils;

import org.opengroove.jzbot.bzf.ListservConnector;
import org.opengroove.jzbot.bzf.Server;
import org.opengroove.jzbot.fact.ArgumentList;
import org.opengroove.jzbot.fact.FactContext;
import org.opengroove.jzbot.fact.FactoidException;
import org.opengroove.jzbot.fact.Function;

public class BzflistFunction extends Function
{
    
    @Override
    public String evaluate(ArgumentList arguments, FactContext context)
    {
        try
        {
            Server[] servers = ListservConnector.getServers();
            String prefix = arguments.get(0);
            String delimiter = "";
            if (arguments.length() > 2)
                delimiter = arguments.get(2);
            ArrayList<String> results = new ArrayList<String>();
            for (Server server : servers)
            {
                setVars(context.getLocalVars(), server, prefix);
                results.add(arguments.resolve(1));
                if ("1".equals(context.getLocalVars().get(prefix + "-quit")))
                    break;
            }
            return StringUtils.delimited(results.toArray(new String[0]), delimiter);
        }
        catch (Exception e)
        {
            throw new FactoidException("Exception occured while running {{bzflist}}", e);
        }
    }
    
    private void setVars(Map<String, String> vars, Server server, String prefix)
    {
        server.loadIntoVars(vars, prefix);
    }
    
    @Override
    public String getHelp(String topic)
    {
        return "Syntax: {{bzflist||<prefix>||<action>||<delimiter>}} -- Contacts the "
                + "public BZFlag list server and retrieves a list of all servers. For each "
                + "server, <action> is then invoked, with several variables starting with "
                + "\"<prefix>-\" set. The best way to get a list of all of these variables "
                + "is to run {{bzflist}} with the action being {{llvars||<prefix>-}}.\n"
                + "If the action sets a variable called <prefix>-quit to the value \"1\", "
                + "{{bzflist}} will stop iterating over servers and return immediately. "
                + "{{bzflist}} evaluates to what its actions evaluated to, separated "
                + "by <delimiter>. Unlike most iterating functions, \"<prefix>-\" variables "
                + "set during iteration will not be deleted afterward.";
    }
    
}
