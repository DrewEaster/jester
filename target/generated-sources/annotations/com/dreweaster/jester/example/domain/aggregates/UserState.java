package com.dreweaster.jester.example.domain.aggregates;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Generated;

/**
 * Immutable implementation of {@link AbstractUserState}.
 * <p>
 * Use the builder to create immutable instances:
 * {@code UserState.builder()}.
 */
@SuppressWarnings({"all"})
@Generated({"Immutables.generator", "AbstractUserState"})
public final class UserState extends AbstractUserState {
  private final String username;
  private final String password;

  private UserState(String username, String password) {
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
   * Copy the current immutable object by setting a value for the {@link AbstractUserState#username() username} attribute.
   * An equals check used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for username
   * @return A modified copy of the {@code this} object
   */
  public final UserState withUsername(String value) {
    if (this.username.equals(value)) return this;
    String newValue = Objects.requireNonNull(value, "username");
    return new UserState(newValue, this.password);
  }

  /**
   * Copy the current immutable object by setting a value for the {@link AbstractUserState#password() password} attribute.
   * An equals check used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for password
   * @return A modified copy of the {@code this} object
   */
  public final UserState withPassword(String value) {
    if (this.password.equals(value)) return this;
    String newValue = Objects.requireNonNull(value, "password");
    return new UserState(this.username, newValue);
  }

  /**
   * This instance is equal to all instances of {@code UserState} that have equal attribute values.
   * @return {@code true} if {@code this} is equal to {@code another} instance
   */
  @Override
  public boolean equals(Object another) {
    if (this == another) return true;
    return another instanceof UserState
        && equalTo((UserState) another);
  }

  private boolean equalTo(UserState another) {
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
   * Prints the immutable value {@code UserState} with attribute values.
   * @return A string representation of the value
   */
  @Override
  public String toString() {
    return "UserState{"
        + "username=" + username
        + ", password=" + password
        + "}";
  }

  /**
   * Creates an immutable copy of a {@link AbstractUserState} value.
   * Uses accessors to get values to initialize the new immutable instance.
   * If an instance is already immutable, it is returned as is.
   * @param instance The instance to copy
   * @return A copied immutable UserState instance
   */
  public static UserState copyOf(AbstractUserState instance) {
    if (instance instanceof UserState) {
      return (UserState) instance;
    }
    return UserState.builder()
        .from(instance)
        .create();
  }

  /**
   * Creates a builder for {@link UserState UserState}.
   * @return A new UserState builder
   */
  public static UserState.Builder builder() {
    return new UserState.Builder();
  }

  /**
   * Builds instances of type {@link UserState UserState}.
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
     * Fill a builder with attribute values from the provided {@code AbstractUserState} instance.
     * Regular attribute values will be replaced with those from the given instance.
     * Absent optional values will not replace present values.
     * @param instance The instance from which to copy values
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder from(AbstractUserState instance) {
      Objects.requireNonNull(instance, "instance");
      username(instance.username());
      password(instance.password());
      return this;
    }

    /**
     * Initializes the value for the {@link AbstractUserState#username() username} attribute.
     * @param username The value for username 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder username(String username) {
      this.username = Objects.requireNonNull(username, "username");
      initBits &= ~INIT_BIT_USERNAME;
      return this;
    }

    /**
     * Initializes the value for the {@link AbstractUserState#password() password} attribute.
     * @param password The value for password 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder password(String password) {
      this.password = Objects.requireNonNull(password, "password");
      initBits &= ~INIT_BIT_PASSWORD;
      return this;
    }

    /**
     * Builds a new {@link UserState UserState}.
     * @return An immutable instance of UserState
     * @throws java.lang.IllegalStateException if any required attributes are missing
     */
    public UserState create() {
      if (initBits != 0) {
        throw new IllegalStateException(formatRequiredAttributesMessage());
      }
      return new UserState(username, password);
    }

    private String formatRequiredAttributesMessage() {
      List<String> attributes = new ArrayList<String>();
      if ((initBits & INIT_BIT_USERNAME) != 0) attributes.add("username");
      if ((initBits & INIT_BIT_PASSWORD) != 0) attributes.add("password");
      return "Cannot build UserState, some of required attributes are not set " + attributes;
    }
  }
}
