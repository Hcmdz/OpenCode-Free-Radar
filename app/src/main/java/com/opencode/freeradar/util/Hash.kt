/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.util

import java.security.MessageDigest

fun sha256Hex(text: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it) }
}
