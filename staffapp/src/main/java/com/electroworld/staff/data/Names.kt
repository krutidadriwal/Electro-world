package com.electroworld.staff.data

// People's names are always shown and saved in Title Case ("ramesh KUMAR"
// -> "Ramesh Kumar"), whatever case they were typed or stored in. A word
// starts after anything that isn't a letter or apostrophe, so "mary-jane"
// -> "Mary-Jane" and "k.ramesh" -> "K.Ramesh", but "o'brien" -> "O'brien".
// Same rules as the customer app's Names.kt.

// For text fields, on every keystroke: only changes letter case, never the
// length, so the cursor stays put and spaces typed so far are kept.
fun String.titleCaseAsTyped(): String {
  val result = StringBuilder(length)
  var startOfWord = true
  for (c in this) {
    result.append(if (startOfWord) c.uppercaseChar() else c.lowercaseChar())
    startOfWord = !c.isLetter() && c != '\''
  }
  return result.toString()
}

// For saving and showing: also trims and collapses runs of spaces.
fun String.toTitleCase(): String = trim().replace(Regex("\\s+"), " ").titleCaseAsTyped()
