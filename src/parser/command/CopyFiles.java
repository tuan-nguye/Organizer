package parser.command;

import parser.option.Option;

import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public class CopyFiles extends Command {
    @Override
    public void execute(List<String> args, Set<Option> options) {
        if(args.size() != 2) throw new IllegalArgumentException("source or destination missing");
        System.out.println("source=" + args.get(0));
        System.out.println("destination=" + args.get(1));
    }
}
