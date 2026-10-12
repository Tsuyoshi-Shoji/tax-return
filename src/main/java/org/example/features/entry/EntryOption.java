package org.example.features.entry;

public class EntryOption {
    private final String id;
    private final String label;
    private final String group;

    public EntryOption(String id, String label, String group) {
        this.id = id;
        this.label = label;
        this.group = group;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getGroup() {
        return group;
    }
}
