public class ParaInit_Binary_Encode {
    static class TreeNode {
        String code;
        TreeNode left;
        TreeNode right;

        TreeNode(String code) {
            this.code = code;
        }
    }

    public static TreeNode buildTree(String code, int depth, int maxDepth) {
        TreeNode node = new TreeNode(code);
        if (depth < maxDepth) {
            node.left = buildTree(code + "0", depth + 1, maxDepth);
            node.right = buildTree(code + "1", depth + 1, maxDepth);
        }
        return node;
    }

    public static void main(String[] args) {
        int maxDepth = 3;
        TreeNode root = buildTree("", 0, maxDepth);
        System.out.println("Binary tree encoding constructed with max depth: " + maxDepth);
    }
}
