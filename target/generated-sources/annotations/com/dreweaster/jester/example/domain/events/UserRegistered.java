package com.dreweaster.jester.example.domain.events;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Generated;

/**
 * Immutable implementation of {@link AbstractUserRegistered}.
 * <p>
 * Use the builder to create immutable instances:
 * {@code UserRegistered.builder()}.
 */
@SuppressWarnings({"all"})
@Generated({"Immutables.generator", "AbstractUserRegistered"})
public final class UserRegistered extends AbstractUserRegistered {
  private final String username;
  private final String password;

  private UserRegistered(String username, String password) {
    this.username = username;
    this.password = password;
  }

  /**
   * @return The value of the {@code username} attribute
   */
  @Override
  public String username() {
    return username;
  }

  /**
   * @return The value of the {@code password} attribute
   */
  @Override
  public String password() {
    return password;
  }

  /**
   * Copy the current immutable object by setting a value for the {@link AbstractUserRegistered#username() username} attribute.
   * An equals check used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for username
   * @return A modified copy of the {@code this} object
   */
  public final UserRegistered withUsername(String value) {
    if (this.username.equals(value)) return this;
    String newValue = Objects.requireNonNull(value, "username");
    return new UserRegistered(newValue, this.password);
  }

  /**
   * Copy the current immutable object by setting a value for the {@link AbstractUserRegistered#password() password} attribute.
   * An equals check used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for password
   * @return A modified copy of the {@code this} object
   */
  public final UserRegistered withPassword(String value) {
    if (this.password.equals(value)) return this;
    String newValue = Objects.requireNonNull(value, "password");
    return new UserRegistered(this.username, newValue);
  }

  /**
   * This instance is equal to all instances of {@code UserRegistered} that have equal attribute values.
   * @return {@code true} if {@code this} is equal to {@code another} instance
   */
  @Override
  public boolean equals(Object another) {
    if (this == another) return true;
    return another instanceof UserRegistered
        && equalTo((UserRegistered) another);
  }

  private boolean equalTo(UserRegistered another) {
    return username.equals(another.username)
        && password.equals(another.password);
  }

  /**
   * Computes a hash code from attributes: {@code username}, {@code password}.
   * @return hashCode value
   */
  @Override
  public int hashCode() {
    int h = 31;
    h = h * 17 + username.hashCode();
    h = h * 17 + password.hashCode();
    return h;
  }

  /**
   * Prints the immutable value {@code UserRegistered} with attribute values.
   * @return A string representation of the value
   */
  @Override
  public String toString() {
    return "UserRegistered{"
        + "username=" + username
        + ", password=" + password
        + "}";
  }

  /**
   * Creates an immutable copy of a {@link AbstractUserRegistered} value.
   * Uses accessors to get values to initialize the new immutable instance.
   * If an instance is already immutable, it is returned as is.
   * @param instance The instance to copy
   * @return A copied immutable UserRegistered instance
   */
  public static UserRegistered copyOf(AbstractUserRegistered instance) {
    if (instance instanceof UserRegistered) {
      return (UserRegistered) instance;
    }
    return UserRegistered.builder()
        .from(instance)
        .create();
  }

  /**
   * Creates a builder for {@link UserRegistered UserRegistered}.
   * @return A new UserRegistered builder
   */
  public static UserRegistered.Builder builder() {
    return new UserRegistered.Builder();
  }

  /**
   * Builds instances of type {@link UserRegistered UserRegistered}.
   * Initialize attributes and then invoke the {@link #create()} method to create an
   * immutable instance.
   * <p><em>{@code Builder} is not thread-safe and generally should not be stored in a field or collection,
   * but instead used immediately to create instances.</em>
   */
  public static final class Builder {
    private static final long INIT_BIT_USERNAME = 0x1L;
    private static final long INIT_BIT_PASSWORD = 0x2L;
    private long initBits = 0x3L;

    private String username;
    private String password;

    private Builder() {
    }

    /**
     * Fill a builder with attribute values from the provided {@code AbstractUserRegistered} instance.
     * Regular attribute values will be replaced with those from the given instance.
     * Absent optional values will not replace present values.
     * @param instance The instance from which to copy values
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder from(AbstractUserRegistered instance) {
      Objects.requireNonNull(instance, "instance");
      username(instance.username());
      password(instance.password());
      return this;
    }

    /**
     * Initializes the value for the {@link AbstractUserRegistered#username() username} attribute.
     * @param username The value for username 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder username(String username) {
      this.username = Objects.requireNonNull(username, "username");
      initBits &= ~INIT_BIT_USERNAME;
      return this;
    }

    /**
     * Initializes the value for the {@link AbstractUserRegistered#password() password} attribute.
     * @param password The value for password 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder password(String password) {
      this.password = Objects.requireNonNull(password, "password");
      initBits &= ~INIT_BIT_PASSWORD;
      return this;
    }

    /**
     * Builds a new {@link UserRegistered UserRegistered}.
     * @return An immutable instance of UserRegistered
     * @throws java.lang.IllegalStateException if any required attributes are missing
     */
    public UserRegistered create() {
      if (initBits != 0) {
        throw new IllegalStateException(formatRequiredAttributesMessage());
      }
      return new UserRegistered(username, password);
    }

    private String formatRequiredAttributesMessage() {
      List<String> attributes = new ArrayList<String>();
      if ((initBits & INIT_BIT_USERNAME) != 0) attributes.add("username");
      if ((initBits & INIT_BIT_PASSWORD) != 0) attributes.add("password");
      return "Cannot build UserRegistered, some of required attributes are not set " + attributes;
    }
  }
}
