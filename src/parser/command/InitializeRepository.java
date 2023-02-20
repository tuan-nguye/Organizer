package parser.command;

import parser.Configuration;
import parser.option.Option;
import parser.option.ValueOption;

import java.util.Map;

public class InitializeRepository extends Command {
    private final int DEFAULT_FOLDER_SIZE = 500;

    @Override
    public void execute(String[] args, Configuration config) {
        int folderSize = getFolderSize(config);
        System.out.println("initializing repo with size=" + folderSize + "... beep beep boop");
    }

    private int getFolderSize(Configuration config) {
        Map<String, Option> optionMap = config.allOptions();
        if(optionMap.containsKey("folderSize")) {
            ValueOption folderSizeOption = (ValueOption) optionMap.get("folderSize");
            return Integer.parseInt(folderSizeOption.getValues().get(0));
        } else {
            return DEFAULT_FOLDER_SIZE;
        }
    }
}
