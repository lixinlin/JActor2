public class M2 {
    public static void main(String[] args) {
        Reply<Void> reply = new Reply<Void>() {
            @Override
            public void response(Void value) {
                System.out.println("end");
            }
        };
        new A2().begin(reply, new B2());
    }
}