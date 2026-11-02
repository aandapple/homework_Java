public class Main {

    public static void main(String[] args) {
        System.out.println("===== Balance =====");
        Balance thousand = Balance.of(1000, 0, Currency.RUB);
        Balance fifty = Balance.of(50_50, Currency.RUB);            // 50.50 ₽ в копейках
        System.out.println("1000 + 50.50 = " + thousand.add(fifty));
        System.out.println("1000 - 50.50 = " + thousand.subtract(fifty));
        System.out.println("Исходный объект не изменился: " + thousand);
        System.out.println("100 $ в рублях: " + Balance.of(100, 0, Currency.USD).convertTo(Currency.RUB));
        System.out.println("10 000 ₽ в евро: " + Balance.of(10_000, 0, Currency.RUB).convertTo(Currency.EUR));
        System.out.println("10 000 ₽ в юанях: " + Balance.of(10_000, 0, Currency.RUB).convertTo(Currency.CNY));
        System.out.println("equals: " + Balance.of(5, 0, Currency.RUB).equals(Balance.of(500, Currency.RUB)));

        System.out.println();
        System.out.println("===== Операции с разными валютами =====");
        System.out.println("1000 ₽ + 1 $ = " + thousand.add(Balance.of(1, 0, Currency.USD)));
        System.out.println("1000 ₽ - 2 € = " + thousand.subtract(Balance.of(2, 0, Currency.EUR)));
        System.out.println("1000 ₽ + 10.00 ¥ (через число и валюту) = " + thousand.add(10_00, Currency.CNY));
        System.out.println("10 $ + 960 ₽ = " + Balance.of(10, 0, Currency.USD).add(Balance.of(960, 0, Currency.RUB)));
        System.out.println("100 € больше, чем 100 $? "
                + Balance.of(100, 0, Currency.EUR).isGreaterThan(Balance.of(100, 0, Currency.USD)));

        System.out.println();
        System.out.println("===== Некорректные операции с Balance =====");
        tryRun("Отрицательная сумма", () -> Balance.of(-1, Currency.RUB));
        tryRun("Прибавить отрицательное", () -> thousand.add(-500));
        tryRun("Вычесть больше, чем есть", () -> fifty.subtract(thousand));
        tryRun("Вычесть 20 $ из 1000 ₽", () -> thousand.subtract(Balance.of(20, 0, Currency.USD)));
        tryRun("Прибавить 1 копейку к долларам", () -> Balance.zero(Currency.USD).add(1, Currency.RUB));
        tryRun("Валюта null", () -> Balance.of(100, null));

        System.out.println();
        System.out.println("===== Проверка номера счёта =====");
        tryRun("Номер короче 20 цифр",
                () -> new BankAccount("40817810", "Ксения", Currency.RUB, Balance.of(100, 0, Currency.RUB)));
        tryRun("Номер с буквой",
                () -> new BankAccount("4081781000000000000А", "Ксения", Currency.RUB, Balance.of(100, 0, Currency.RUB)));
        tryRun("Номер с пробелами",
                () -> new BankAccount("40817 810 0000 0000 0001", "Ксения", Currency.RUB, Balance.of(100, 0, Currency.RUB)));
        tryRun("Номер null",
                () -> new BankAccount(null, "Ксения", Currency.RUB, Balance.of(100, 0, Currency.RUB)));
        tryRun("Пустое имя владельца",
                () -> new BankAccount("40817810000000000001", " ", Currency.RUB, Balance.of(100, 0, Currency.RUB)));

        System.out.println();
        System.out.println("===== BankAccount =====");
        BankAccount ksenia = new BankAccount("40817810000000000001", "Ксения",
                Currency.RUB, Balance.of(20_000, 0, Currency.RUB));
        BankAccount anna = new BankAccount("40817840000000000002", "Анна",
                Currency.USD, Balance.of(500, 0, Currency.USD));

        ksenia.deposit(Balance.of(50_000, 0, Currency.RUB));
        ksenia.deposit(Balance.of(100, 0, Currency.USD));            // пополнение в долларах
        ksenia.withdraw(Balance.of(3_000, 0, Currency.RUB));
        ksenia.transferTo(anna, Balance.of(9_600, 0, Currency.RUB)); // придёт ровно 100 $
        System.out.println(ksenia);
        System.out.println(anna);

        System.out.println();
        System.out.println("===== Некорректные операции со счетами =====");
        tryRun("Перевод больше лимита", () -> ksenia.transferTo(anna, Balance.of(25_000, 0, Currency.RUB)));
        tryRun("Недостаточно средств", () -> anna.withdraw(Balance.of(300, 0, Currency.USD)));
        tryRun("Перевод самому себе", () -> ksenia.transferTo(ksenia, Balance.of(100, 0, Currency.RUB)));
        tryRun("Нулевая сумма", () -> ksenia.deposit(Balance.zero(Currency.RUB)));
        tryRun("Получатель null", () -> ksenia.transferTo(null, Balance.of(100, 0, Currency.RUB)));

        anna.freeze();
        tryRun("Перевод на заблокированный счёт",
                () -> ksenia.transferTo(anna, Balance.of(1_000, 0, Currency.RUB)));
        System.out.println("Баланс Ксении не изменился после ошибки: " + ksenia.getBalance());
        anna.unfreeze();

        ksenia.setWithdrawalLimit(Balance.of(30_000, 0, Currency.RUB));
        ksenia.transferTo(anna, Balance.of(25_000, 0, Currency.RUB)); // теперь проходит
        System.out.println(ksenia);
        System.out.println(anna);

        System.out.println();
        System.out.println("===== История счёта " + ksenia.getMaskedAccountNumber() + " =====");
        ksenia.getHistory().forEach(entry -> System.out.println("  " + entry));
        System.out.println("===== История счёта " + anna.getMaskedAccountNumber() + " =====");
        anna.getHistory().forEach(entry -> System.out.println("  " + entry));
    }

    /** Выполняет действие и печатает исключение, если оно было. */
    private static void tryRun(String description, Runnable action) {
        try {
            action.run();
            System.out.println(description + ": ошибки нет (а должна была быть!)");
        } catch (RuntimeException e) {
            System.out.println(description + ": " + e.getClass().getSimpleName() + " — " + e.getMessage());
        }
    }
}
