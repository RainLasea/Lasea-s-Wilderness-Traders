package com.abysslasea.wildernesstraders.dialogue;

import java.util.Map;

public class DialogueNode {
    private final String textKey;
    private final Map<Integer, String> optionKeys;
    private final Map<Integer, Integer> options;

    public DialogueNode(String textKey, Map<Integer, String> optionKeys, Map<Integer, Integer> options) {
        this.textKey = textKey;
        this.optionKeys = optionKeys;
        this.options = options;
    }

    public String getTextKey() {
        return textKey;
    }

    public Map<Integer, String> getOptionKeys() {
        return optionKeys;
    }

    public Map<Integer, Integer> getOptions() {
        return options;
    }
}