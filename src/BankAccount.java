import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Банковский счёт.
 *
 * Поля сверх баланса:
 *  - withdrawalLimit — максимальная сумма одного списания (снятия или перевода);
 *  - frozen — заблокирован ли счёт (по заблокированному счёту нельзя проводить операции);
 *  - history — история операций.
 */
public class BankAccount {

    private final String accountNumber;
    private final String owner;
    private Balance balance;            // сам Balance неизменяемый, при операциях заменяем ссылку
    private Balance withdrawalLimit;
    private boolean frozen;
    private final List<String> history = new ArrayList<>();

    public BankAccount(final String accountNumber, final String owner,
                       final Currency currency, final Balance withdrawalLimit) {
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("Номер счёта не может быть пустым");
        }
        if (owner == null || owner.isBlank()) {
            throw new IllegalArgumentException("Владелец не может быть пустым");
        }
        if (currency == null) {
            throw new IllegalArgumentException("Валюта счёта не может быть null");
        }
        this.accountNumber = accountNumber;
        this.owner = owner;
        this.balance = Balance.zero(currency);
        setWithdrawalLimit(withdrawalLimit);
        log("Счёт открыт, лимит списания " + this.withdrawalLimit);
    }

    // ---------- Операции с деньгами ----------

    /** Пополнение. Сумма в другой валюте автоматически конвертируется в валюту счёта. */
    public void deposit(final Balance amount) {
        requireActive();
        Balance converted = toAccountCurrency(amount);
        requirePositive(converted);
        balance = balance.add(converted);
        log("Пополнение +" + converted + describeConversion(amount, converted));
    }

    /** Снятие с учётом лимита и остатка. */
    public void withdraw(final Balance amount) {
        requireActive();
        Balance converted = toAccountCurrency(amount);
        checkCanWithdraw(converted);
        balance = balance.subtract(converted);
        log("Снятие -" + converted + describeConversion(amount, converted));
    }

    /**
     * Перевод на другой счёт. Сумма задаётся в валюте этого (отправляющего) счёта
     * и при необходимости конвертируется в валюту получателя.
     * Сначала проверяем всё, и только потом меняем оба баланса — чтобы при ошибке
     * деньги не списались с одного счёта и не пропали.
     */
    public void transferTo(final BankAccount target, final Balance amount) {
        if (target == null) {
            throw new IllegalArgumentException("Счёт получателя не может быть null");
        }
        if (target == this) {
            throw new IllegalArgumentException("Нельзя перевести деньги на тот же самый счёт");
        }
        requireActive();
        target.requireActive();

        Balance outgoing = toAccountCurrency(amount);
        checkCanWithdraw(outgoing);
        Balance incoming = outgoing.convertTo(target.balance.getCurrency());

        Balance newSourceBalance = balance.subtract(outgoing);
        Balance newTargetBalance = target.balance.add(incoming);

        balance = newSourceBalance;
        target.balance = newTargetBalance;

        log("Перевод -" + outgoing + " на счёт " + target.accountNumber);
        target.log("Перевод +" + incoming + " со счёта " + accountNumber
                + describeConversion(outgoing, incoming));
    }

    // ---------- Лимит и блокировка ----------

    public void setWithdrawalLimit(final Balance limit) {
        if (limit == null) {
            throw new IllegalArgumentException("Лимит не может быть null");
        }
        Balance converted = limit.convertTo(balance.getCurrency());
        requirePositive(converted);
        this.withdrawalLimit = converted;
        if (!history.isEmpty()) {
            log("Новый лимит списания " + converted);
        }
    }

    public void freeze() {
        if (frozen) {
            throw new IllegalStateException("Счёт " + accountNumber + " уже заблокирован");
        }
        frozen = true;
        log("Счёт заблокирован");
    }

    public void unfreeze() {
        if (!frozen) {
            throw new IllegalStateException("Счёт " + accountNumber + " не заблокирован");
        }
        frozen = false;
        log("Счёт разблокирован");
    }

    // ---------- Проверки ----------

    private void checkCanWithdraw(final Balance amount) {
        requirePositive(amount);
        if (amount.isGreaterThan(withdrawalLimit)) {
            throw new IllegalStateException("Сумма " + amount + " превышает лимит списания "
                    + withdrawalLimit + " по счёту " + accountNumber);
        }
        if (amount.isGreaterThan(balance)) {
            throw new IllegalStateException("Недостаточно средств на счёте " + accountNumber
                    + ": есть " + balance + ", нужно " + amount);
        }
    }

    private void requireActive() {
        if (frozen) {
            throw new IllegalStateException("Счёт " + accountNumber + " заблокирован");
        }
    }

    private static void requirePositive(final Balance amount) {
        if (amount.isZero()) {
            throw new IllegalArgumentException("Сумма операции должна быть больше нуля");
        }
    }

    private Balance toAccountCurrency(final Balance amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Сумма не может быть null");
        }
        return amount.convertTo(balance.getCurrency());
    }

    private static String describeConversion(final Balance original, final Balance converted) {
        return original.getCurrency() == converted.getCurrency()
                ? ""
                : " (конвертировано из " + original + ")";
    }

    private void log(final String message) {
        history.add(message);
    }

    // ---------- Геттеры ----------

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getOwner() {
        return owner;
    }

    public Balance getBalance() {
        return balance;
    }

    public Balance getWithdrawalLimit() {
        return withdrawalLimit;
    }

    public boolean isFrozen() {
        return frozen;
    }

    /** Возвращаем копию только для чтения, чтобы историю нельзя было изменить снаружи. */
    public List<String> getHistory() {
        return Collections.unmodifiableList(history);
    }

    // ---------- equals / hashCode / toString ----------

    /** Счета равны, если у них одинаковый номер. */
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BankAccount other)) {
            return false;
        }
        return accountNumber.equals(other.accountNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountNumber);
    }

    @Override
    public String toString() {
        return "Счёт " + accountNumber + " (" + owner + "): " + balance
                + ", лимит " + withdrawalLimit + (frozen ? ", ЗАБЛОКИРОВАН" : "");
    }
}
