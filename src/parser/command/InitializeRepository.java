package parser.command;

import parser.option.Option;

import java.util.List;
import java.util.Set;

public class InitializeRepository extends Command {
    private final int DEFAULT_FOLDER_SIZE = 500;

    @Override
    public void execute(List<String> args, Set<Option> options) {
        System.out.println("initializing repo with size=" + getFolderSize(options) + "... beep beep boop");
        // create hidden config file if it doesn't exist
    }

    private int getFolderSize(Set<Option> options) {
        for(Option option : options) {
            if(option.getName().equals("folderSize")) {

            }
        }

        return DEFAULT_FOLDER_SIZE;
    }
}
