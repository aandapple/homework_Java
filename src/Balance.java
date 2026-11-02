import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Неизменяемая денежная сумма в конкретной валюте.
 *
 * Сумма хранится в минимальных единицах валюты (копейки, центы, фэни) типом long,
 * а не double: 0.1 + 0.2 != 0.3 в double, а деньги должны считаться точно.
 *
 * Любая операция (add, subtract, convertTo) не меняет текущий объект,
 * а возвращает новый Balance.
 */
public final class Balance implements Comparable<Balance> {

    /** Во всех валютах из enum Currency в одной основной единице 100 минимальных. */
    private static final int MINOR_UNITS_IN_MAJOR = 100;

    private final long amount;          // в минимальных единицах, всегда >= 0
    private final Currency currency;

    private Balance(final long amount, final Currency currency) {
        if (currency == null) {
            throw new IllegalArgumentException("Валюта не может быть null");
        }
        if (amount < 0) {
            throw new IllegalArgumentException("Сумма не может быть отрицательной: " + amount);
        }
        this.amount = amount;
        this.currency = currency;
    }

    // ---------- Фабричные методы ----------

    /** Сумма в минимальных единицах: Balance.of(150_050, RUB) = 1 500,50 ₽. */
    public static Balance of(final long minorUnits, final Currency currency) {
        return new Balance(minorUnits, currency);
    }

    /** Сумма в основных единицах с копейками: Balance.of(1500, 50, RUB) = 1 500,50 ₽. */
    public static Balance of(final long major, final int minor, final Currency currency) {
        if (major < 0) {
            throw new IllegalArgumentException("Сумма не может быть отрицательной: " + major);
        }
        if (minor < 0 || minor >= MINOR_UNITS_IN_MAJOR) {
            throw new IllegalArgumentException("Копейки должны быть от 0 до 99: " + minor);
        }
        long total = Math.addExact(Math.multiplyExact(major, MINOR_UNITS_IN_MAJOR), minor);
        return new Balance(total, currency);
    }

    public static Balance zero(final Currency currency) {
        return new Balance(0, currency);
    }

    // ---------- Денежные операции ----------

    public Balance add(final Balance other) {
        requireSameCurrency(other);
        return add(other.amount);
    }

    public Balance add(final long minorUnits) {
        if (minorUnits < 0) {
            throw new IllegalArgumentException("Нельзя прибавить отрицательную сумму: " + minorUnits);
        }
        // addExact бросит ArithmeticException при переполнении long, а не тихо уйдёт в минус
        return new Balance(Math.addExact(amount, minorUnits), currency);
    }

    public Balance subtract(final Balance other) {
        requireSameCurrency(other);
        return subtract(other.amount);
    }

    public Balance subtract(final long minorUnits) {
        if (minorUnits < 0) {
            throw new IllegalArgumentException("Нельзя вычесть отрицательную сумму: " + minorUnits);
        }
        if (minorUnits > amount) {
            throw new IllegalStateException("Недостаточно средств: есть " + this
                    + ", нужно " + new Balance(minorUnits, currency));
        }
        return new Balance(amount - minorUnits, currency);
    }

    /**
     * Конвертация по курсам из Currency через рубли.
     * сумма_в_новой_валюте = сумма * курс_старой / курс_новой.
     * Округляем банковским округлением (HALF_EVEN) до минимальной единицы.
     */
    public Balance convertTo(final Currency target) {
        if (target == null) {
            throw new IllegalArgumentException("Валюта для конвертации не может быть null");
        }
        if (target == currency) {
            return this;
        }
        BigDecimal converted = BigDecimal.valueOf(amount)
                .multiply(BigDecimal.valueOf(currency.getPriceInRublesKopecks()))
                .divide(BigDecimal.valueOf(target.getPriceInRublesKopecks()), 0, RoundingMode.HALF_EVEN);
        return new Balance(converted.longValueExact(), target);
    }

    // ---------- Сравнения ----------

    public boolean isZero() {
        return amount == 0;
    }

    public boolean isGreaterThan(final Balance other) {
        return compareTo(other) > 0;
    }

    @Override
    public int compareTo(final Balance other) {
        requireSameCurrency(other);
        return Long.compare(amount, other.amount);
    }

    private void requireSameCurrency(final Balance other) {
        if (other == null) {
            throw new IllegalArgumentException("Сумма не может быть null");
        }
        if (other.currency != currency) {
            throw new IllegalArgumentException("Разные валюты: " + currency.getCode()
                    + " и " + other.currency.getCode() + ". Сначала сделайте convertTo()");
        }
    }

    // ---------- Геттеры ----------

    public long getAmount() {
        return amount;
    }

    public Currency getCurrency() {
        return currency;
    }

    // ---------- equals / hashCode / toString ----------

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Balance other)) {
            return false;
        }
        return amount == other.amount && currency == other.currency;
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, currency);
    }

    /** Например: "1500.50 ₽", "12.05 $". */
    @Override
    public String toString() {
        return String.format("%d.%02d %s",
                amount / MINOR_UNITS_IN_MAJOR, amount % MINOR_UNITS_IN_MAJOR, currency.getSymbol());
    }
}
