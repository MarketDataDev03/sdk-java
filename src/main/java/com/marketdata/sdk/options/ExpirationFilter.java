package com.marketdata.sdk.options;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Mutually-exclusive expiration filter for {@code /v1/options/chain/}. The chain endpoint's
 * expiration-side parameters ({@code expiration}, {@code dte}, {@code from}/{@code to}, {@code
 * month}/{@code year}) cover overlapping selection axes; combining them produces undefined behavior
 * server-side. Modeling them as variants of a sealed interface with a single {@code
 * expirationFilter(...)} setter on the request builder makes that exclusivity compiler-enforced:
 * there is no way to assign two variants at once.
 *
 * <p>Additive expiration-type predicates ({@code weekly}/{@code monthly}/{@code quarterly}/{@code
 * am}/{@code pm}) are not part of this hierarchy — they intersect freely with any variant and stay
 * as separate booleans on the request builder.
 *
 * <p>{@code dte} carries three distinct wire syntaxes on a single string-typed query parameter — a
 * number, a dashed range, or a comma-separated list — modeled as three separate variants ({@link
 * #dte}, {@link #dteRange}, {@link #dteList}) rather than one variant with a formatting branch, so
 * each stays independently constructible and testable.
 */
public sealed interface ExpirationFilter
    permits ExpirationFilter.OnDate,
        ExpirationFilter.Dte,
        ExpirationFilter.DteRange,
        ExpirationFilter.DteList,
        ExpirationFilter.Between,
        ExpirationFilter.MonthYear,
        ExpirationFilter.All {

  /** A specific expiration date — wire form {@code ?expiration=YYYY-MM-DD}. */
  static OnDate onDate(LocalDate date) {
    return new OnDate(date);
  }

  /**
   * Every available expiration — wire form {@code ?expiration=all}.
   *
   * <p>This is <em>not</em> equivalent to leaving the expiration filter unset: with no filter the
   * chain endpoint returns only the front-month (nearest) expiration, whereas {@code all()} returns
   * the full chain across every expiration. The additive {@code weekly}/{@code monthly}/{@code
   * quarterly} predicates still narrow the result on top of it.
   */
  static All all() {
    return new All();
  }

  /** Days-to-expiration filter — wire form {@code ?dte=N}. */
  static Dte dte(int days) {
    requireNonNegativeDte(days);
    return new Dte(days);
  }

  /**
   * Days-to-expiration range filter — wire form {@code ?dte=min-max}, e.g. {@code ?dte=30-45}.
   * {@code min} must not exceed {@code max}.
   */
  static DteRange dteRange(int min, int max) {
    requireNonNegativeDte(min);
    requireNonNegativeDte(max);
    if (min > max) {
      throw new IllegalArgumentException("min must be <= max");
    }
    return new DteRange(min, max);
  }

  /**
   * Days-to-expiration list filter — wire form {@code ?dte=a,b,c}, e.g. {@code ?dte=30,45,60}. At
   * least one value is required.
   */
  static DteList dteList(int first, int... rest) {
    requireNonNegativeDte(first);
    List<Integer> days = new ArrayList<>();
    days.add(first);
    for (int day : rest) {
      requireNonNegativeDte(day);
      days.add(day);
    }
    return new DteList(List.copyOf(days));
  }

  private static void requireNonNegativeDte(int days) {
    if (days < 0) {
      throw new IllegalArgumentException("dte must be non-negative");
    }
  }

  /**
   * Inclusive date range — wire form {@code ?from=YYYY-MM-DD&to=YYYY-MM-DD}. {@code from} must not
   * be strictly after {@code to}.
   */
  static Between between(LocalDate from, LocalDate to) {
    Objects.requireNonNull(from, "from");
    Objects.requireNonNull(to, "to");
    if (from.isAfter(to)) {
      throw new IllegalArgumentException("from must be on or before to");
    }
    return new Between(from, to);
  }

  /**
   * Calendar month-of-year filter — wire form {@code ?month=M&year=YYYY}. {@code month} is the
   * 1-based calendar month (January = 1).
   */
  static MonthYear monthYear(int year, int month) {
    if (month < 1 || month > 12) {
      throw new IllegalArgumentException("month must be in 1..12");
    }
    return new MonthYear(year, month);
  }

  record OnDate(LocalDate date) implements ExpirationFilter {
    public OnDate {
      Objects.requireNonNull(date, "date");
    }
  }

  record Dte(int days) implements ExpirationFilter {}

  record DteRange(int min, int max) implements ExpirationFilter {}

  record DteList(List<Integer> days) implements ExpirationFilter {}

  record Between(LocalDate from, LocalDate to) implements ExpirationFilter {}

  record MonthYear(int year, int month) implements ExpirationFilter {}

  /** The whole chain across every expiration — see {@link #all()}. Carries no data of its own. */
  record All() implements ExpirationFilter {}
}
