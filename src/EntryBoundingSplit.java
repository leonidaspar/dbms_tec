/**
 * Represents a candidate split of a node's entries during an R*-tree split.
 * It stores two groups of entries (Group A and Group B), each with their own bounding box.
 * These groups are evaluated to determine the best split based on overlap and area.
 */
//Class that splits two EntryBoundingGroup objects into two groups on an axis
public class EntryBoundingSplit {
    private EntryBoundingGroup groupA;
    private EntryBoundingGroup groupB;

    public EntryBoundingSplit(EntryBoundingGroup groupA, EntryBoundingGroup groupB) {
        this.groupA = groupA;
        this.groupB = groupB;
    }
    public EntryBoundingGroup getGroupA() {
        return groupA;
    }
    public EntryBoundingGroup getGroupB() {
        return groupB;
    }
}
