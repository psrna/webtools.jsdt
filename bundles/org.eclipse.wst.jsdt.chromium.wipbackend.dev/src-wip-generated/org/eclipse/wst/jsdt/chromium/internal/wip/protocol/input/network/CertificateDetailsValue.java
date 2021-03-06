// Generated source.
// Generator: org.eclipse.wst.jsdt.chromium.internal.wip.tools.protocolgenerator.Generator
// Origin: http://src.chromium.org/blink/trunk/Source/devtools/protocol.json@<unknown>

package org.eclipse.wst.jsdt.chromium.internal.wip.protocol.input.network;

/**
 Details about a request's certificate.
 */
@org.eclipse.wst.jsdt.chromium.internal.protocolparser.JsonType
public interface CertificateDetailsValue {
  /**
   Certificate subject.
   */
  org.eclipse.wst.jsdt.chromium.internal.wip.protocol.input.network.CertificateSubjectValue subject();

  /**
   Name of the issuing CA.
   */
  String issuer();

  /**
   Certificate valid from date.
   */
  Number/*See org.eclipse.wst.jsdt.chromium.internal.wip.protocol.common.network.TimestampTypedef*/ validFrom();

  /**
   Certificate valid to (expiration) date
   */
  Number/*See org.eclipse.wst.jsdt.chromium.internal.wip.protocol.common.network.TimestampTypedef*/ validTo();

}
