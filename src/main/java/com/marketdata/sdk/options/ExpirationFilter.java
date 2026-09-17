package com.marketdata.sdk.options;

import java.time.LocalDate;
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
 */
public sealed interface ExpirationFilter
    permits ExpirationFilter.OnDate,
        ExpirationFilter.Dte,
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

  /** Days-to-expiration filter, single value — wire form {@code ?dte=N}. */
  static Dte dte(int days) {
    requireNonNegativeDte(days);
    return new Dte(Integer.toString(days));
  }

  /**
   * Days-to-expiration filter, inclusive range — wire form {@code ?dte=min-max} (e.g. {@code
   * 30-45}). {@code minDays} must not exceed {@code maxDays}.
   */
  static Dte dteRange(int minDays, int maxDays) {
    requireNonNegativeDte(minDays);
    requireNonNegativeDte(maxDays);
    if (minDays > maxDays) {
      throw new IllegalArgumentException("min must be <= max");
    }
    return new Dte(minDays + "-" + maxDays);
  }

  /**
   * Days-to-expiration filter, comma-separated list — wire form {@code ?dte=d1,d2,...}. At least
   * one value is required — enforced by the {@code first, rest...} signature, so an empty list is
   * unrepresentable through the public API.
   */
  static Dte dteList(int first, int... rest) {
    requireNonNegativeDte(first);
    StringBuilder wireValue = new StringBuilder(Integer.toString(first));
    for (int day : rest) {
      requireNonNegativeDte(day);
      wireValue.append(',').append(day);
    }
    return new Dte(wireValue.toString());
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

  /**
   * Wire value already rendered for {@code ?dte=} — a single number, a range ({@code min-max}), or
   * a comma-separated list, per {@link #dte(int)}, {@link #dteRange(int, int)}, and {@link
   * #dteList(int, int...)}.
   */
  record Dte(String wireValue) implements ExpirationFilter {}

  record Between(LocalDate from, LocalDate to) implements ExpirationFilter {}

  record MonthYear(int year, int month) implements ExpirationFilter {}

  /** The whole chain across every expiration — see {@link #all()}. Carries no data of its own. */
  record All() implements ExpirationFilter {}
}
