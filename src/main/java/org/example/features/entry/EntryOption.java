package org.example.features.entry;

public class EntryOption {
    private final String id;
    private final String label;
    private final String group;
    private final String taxCategory;

    public EntryOption(String id, String label, String group) {
        this(id, label, group, null);
    }

    public EntryOption(String id, String label, String group, String taxCategory) {
        this.id = id;
        this.label = label;
        this.group = group;
        this.taxCategory = taxCategory;
    }

    /** 収益項目の既定の消費税区分（収益項目以外は null）。 */
    public String getTaxCategory() {
        return taxCategory;
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
