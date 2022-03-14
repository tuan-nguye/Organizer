public class Application {
    public static void main(String[] args) {
        Organizer org = new DefaultOrganizer();
        try {
            org.copyAndOrganize(args[0], args[1]);
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
