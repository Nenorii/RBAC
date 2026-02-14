void main() {
    try {
        User u1 = User.create("Sasha", "Balabanov", "SashaB_312@mail.ru");

        System.out.println(u1.format());
    } catch (Exception e) {
        IO.println(e.getMessage());
    }
}
