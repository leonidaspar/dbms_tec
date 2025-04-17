
/*
    * RStarNode.java,
    The root node is at the top.
    Each non leaf node has multiple children,
    Every child is a node, and the parent keeps a  box that encloses all points in that child.
    At the leaf level, the children are data records, not further nodes.
    each nodes stores:
       A list of bounding boxes -> to mbr
       A list of child pointers -> shows that the child node of it is located at this (x) point

    for the boolean isLeaf:(gpt auto den xero an eine etsi na ginei 
                if yes -->   A leaf node has MBRs that enclose single data points (or small groups) and pointers that reference those data records in datafile.dat.
                if no  -->  An internal node has MBRs that enclose entire subtrees, and pointers that reference other R*-tree nodes in indexfile.dat.
                           )
 */

 import java.util.ArrayList;
 import java.util.List;
 
 public class RStarNode{
     public boolean isLeaf; // if yes -> node’s children are actual data records, or else -> nodes children are other nodes
     public List<BoundingBox> mbrs; // each child equals one extra bounding box 
     public List<Long> childrenPointers;//  references a node in indexfile.dat if isLeaf = false OR else a record in datafile.dat
 
     public RStarNode(boolean isLeaf) {
         this.isLeaf = isLeaf;
         this.mbrs = new ArrayList<>();
         this.childrenPointers = new ArrayList<>();
     }
 
     public void addEntry(BoundingBox box, long pointer){ // adds both a bounding box and a pointer
         mbrs.add(box);
         childrenPointers.add(pointer);
     }
 
     public int mbrSize(){
         return mbrs.size(); // how many bounding boxes or pointers (eine to idio panta kai ta 2) the are
     }
 
     @Override
     public String toString() {
         return "RStarNode{" +
                 "isLeaf=" + isLeaf +
                 ", entries=" + mbrSize() +
                 '}';
     }
 
 }
 