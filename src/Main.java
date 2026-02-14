void main() {
    try {
        User u1 = User.create("Sasha", "Balabanov", "SashaB_312@mail.ru");

        Permission read = new Permission("READ", "users", "Может читать пользователей");
        Permission write = new Permission("WRITE", "reports", "Может писать отчеты");
        Permission delete = new Permission("DELETE", "settings", "Может удалять настройки");

        System.out.println("\nИнформация о правах:");
        System.out.println(read.format());
        System.out.println(write.format());
        System.out.println(delete.format());

        System.out.println("\nПоиск 'RE' в name и 'use' в resource:");
        System.out.println("READ: " + read.matches("RE", "use"));
        System.out.println("WRITE: " + write.matches("RE", "use"));
        System.out.println("DELETE: " + delete.matches("RE", "use"));

        System.out.println("\nПоиск 'WR' в name и 'rep' в resource:");
        System.out.println("READ: " + read.matches("WR", "rep"));
        System.out.println("WRITE: " + write.matches("WR", "rep"));
        System.out.println("DELETE: " + delete.matches("WR", "rep"));

        System.out.println("\nПоиск 'DE' в name и 'set' в resource:");
        System.out.println("READ: " + read.matches("DE", "set"));
        System.out.println("WRITE: " + write.matches("DE", "set"));
        System.out.println("DELETE: " + delete.matches("DE", "set"));

        System.out.println(u1.format());
    } catch (Exception e) {
        IO.println(e.getMessage());
    }
}
