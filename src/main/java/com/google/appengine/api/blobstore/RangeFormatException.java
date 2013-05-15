// Copyright 2010 Google Inc. All Rights Reserved.

// This file has been copied from the Google AppEngine project.
// It is licensed under the Apache License 2.0.

package com.google.appengine.api.blobstore;

/**
 * {@code RangeFormatException} is an unchecked exception that is thrown
 * when an invalid Range header format is provided.
 *
 */
public class RangeFormatException extends RuntimeException {
  public RangeFormatException(String message) {
    super(message);
  }

  public RangeFormatException(String message, Throwable cause) {
    super(message, cause);
  }
}
