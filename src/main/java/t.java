public class t {

    public static void main(String[] args) {
        String main_version = args[0]; // "1.0.0"
        String merge_version = args[1]; // "1.0.0"

        String[] mainV = main_version.split("\\.");
        String[] mergeV = merge_version.split("\\.");
        if (check(mainV, mergeV) == 1) {
            int v = Integer.parseInt(mainV[2]) + 1;
            // commentfor commit
            String builder = mainV[0] + "." + mainV[1] + "." + v;
            System.out.println("auto: ");
            System.out.println(builder);
        } else {
            System.out.println("merge");
            System.out.println(merge_version);
        }

    }

    /**
     * @param mainV  : main branch version split
     * @param mergeV : merged branch version split
     * @return 0: merge is highest
     */
    public static int check(String[] mainV, String[] mergeV) {
        for (int i = 2; i >= 0; i--) {
            int base = Integer.parseInt(mainV[i]);
            int merge = Integer.parseInt(mergeV[i]);
            if (merge > base) {
                return 0;
            }
        }
        return 1;
    }
}
