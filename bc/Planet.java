/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 3.0.12
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package bc;

public enum Planet {
  Earth(0),
  Mars(1);

  public final int swigValue() {
    return swigValue;
  }

  public static Planet swigToEnum(int swigValue) {
    Planet[] swigValues = Planet.class.getEnumConstants();
    if (swigValue < swigValues.length && swigValue >= 0 && swigValues[swigValue].swigValue == swigValue)
      return swigValues[swigValue];
    for (Planet swigEnum : swigValues)
      if (swigEnum.swigValue == swigValue)
        return swigEnum;
    throw new IllegalArgumentException("No enum " + Planet.class + " with value " + swigValue);
  }

  @SuppressWarnings("unused")
  private Planet() {
    this.swigValue = SwigNext.next++;
  }

  @SuppressWarnings("unused")
  private Planet(int swigValue) {
    this.swigValue = swigValue;
    SwigNext.next = swigValue+1;
  }

  @SuppressWarnings("unused")
  private Planet(Planet swigEnum) {
    this.swigValue = swigEnum.swigValue;
    SwigNext.next = this.swigValue+1;
  }

  private final int swigValue;

  private static class SwigNext {
    private static int next = 0;
  }
}

