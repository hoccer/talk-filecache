// Copyright 2010 Google Inc. All Rights Reserved.

// This file has been copied from the Google AppEngine project.
// It is licensed under the Apache License 2.0.

package com.google.appengine.api.blobstore;

/**
 * {@code UnsupportedRangeFormatException} is an unchecked exception that is thrown
 * when an valid but unsupported Range header format is provided.
 *
 */
public class UnsupportedRangeFormatException extends RangeFormatException {
  public UnsupportedRangeFormatException(String message) {
    super(message);
  }

  public UnsupportedRangeFormatException(String message, Throwable cause) {
    super(message, cause);
  }
}
