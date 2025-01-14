/*
 * Copyright 2019 Magnus Madsen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

instance LowerBound[Char] {
    pub def minValue(): Char = Char.minValue()
}

instance UpperBound[Char] {
    pub def maxValue(): Char = Char.maxValue()
}

mod Char {

    ///
    ///  Returns `true` if the given char `c` is an ascii character.
    ///
    pub def isAscii(c: Char): Bool =
        c <= '\u0080'

    ///
    /// Returns `true` if the given char `c` is a letter character.
    ///
    pub def isLetter(c: Char): Bool =
        import static java.lang.Character.isLetter(Char): Bool \ {};
        isLetter(c)

    ///
    /// Returns `true` if the given char `c` is a recognized Unicode digit.
    /// This includes the ASCII range 0..9 but also Arabic-Indic digits, Devagari digits and Fullwidth digits.
    ///
    pub def isDigit(c: Char): Bool =
        import static java.lang.Character.isDigit(Char): Bool \ {};
        isDigit(c)

    ///
    /// Returns `true` if the given char `c` is strictly in the range of ASCII digits 0...9.
    ///
    pub def isAsciiDigit(c: Char): Bool =
        isAscii(c) and isDigit(c)

    ///
    /// Returns `true` if the given char `c` is in the range 0...7.
    ///
    pub def isOctDigit(c: Char): Bool =
        '0' <= c and c <= '7'     // '0'..'7'

    ///
    /// Returns `true` if the given char `c` is in the range 0...F.
    ///
    pub def isHexDigit(c: Char): Bool = match c {
        case i if '0' <= i and i <= '9' => true        // '0'..'9'
        case i if 'A' <= i and i <= 'F' => true        // 'A'..'F'
        case i if 'a' <= i and i <= 'f' => true        // 'a'..'f'
        case _ => false
    }

    ///
    /// Returns `true` if the given char `c` is lowercase.
    ///
    pub def isLowerCase(c: Char): Bool =
        import static java.lang.Character.isLowerCase(Char): Bool \ {};
        isLowerCase(c)

    ///
    /// Returns `true` if the given char `c` is uppercase.
    ///
    pub def isUpperCase(c: Char): Bool =
        import static java.lang.Character.isUpperCase(Char): Bool \ {};
        isUpperCase(c)

    ///
    /// Returns `true` if the given char `c` is a white space character.
    ///
    pub def isWhiteSpace(c: Char): Bool =
        import static java.lang.Character.isWhitespace(Char): Bool \ {};
        isWhitespace(c)

    ///
    /// Converts a letter to its lowercase version.
    ///
    /// Returns the original character if it does not have a lowercase version.
    ///
    pub def toLowerCase(c: Char): Char =
        import static java.lang.Character.toLowerCase(Char): Char \ {};
        toLowerCase(c)

    ///
    /// Converts a letter to its uppercase version.
    ///
    /// Returns the original character if it does not have a uppercase version.
    ///
    pub def toUpperCase(c: Char): Char =
        import static java.lang.Character.toUpperCase(Char): Char \ {};
        toUpperCase(c)

    ///
    /// Returns the character `c` as a string.
    ///
    pub def toString(c: Char): String = ToString.toString(c)

    ///
    /// Returns the character given by the all zero byte.
    ///
    pub def minValue(): Char = '\u0000'

    ///
    /// Returns the character given by the maximum valued byte.
    ///
    pub def maxValue(): Char = '\uffff'

    ///
    /// Returns the integer value representated by Char `c` (e.g. '1' => `Some(1)``)
    /// or `None` if `c` does not represent a number.
    ///
    /// This function cannot handle supplementary characters.
    ///
    pub def getNumericValue(c: Char): Option[Int32] =
        import static java.lang.Character.getNumericValue(Char): Int32 \ {};
        match getNumericValue(c) {
            case i if i < 0 => None
            case i          => Some(i)
        }

    ///
    /// Returns the integer value representated by the codepoint `cp` e.g. codepoint 0x0031
    /// which is the char '1' returns `Some(1)`.
    ///
    /// Returns `None` if `cp` does not represent a number.
    ///
    /// This function handles supplementary characters.
    ///
    pub def getCodePointNumericValue(cp: Int32): Option[Int32] =
        import static java.lang.Character.getNumericValue(Int32): Int32 \ {};
        match getNumericValue(cp) {
            case i if i < 0 => None
            case i          => Some(i)
        }

    ///
    /// Returns the integer value representated by Char `c` with respect to `radix`. E.g.
    /// `digit(radix = 10, '1') => Some(1)`
    /// `digit(radix = 16, 'a') => Some(11)`
    ///
    /// Returns `None` if `cp` does not represent a number.
    ///
    pub def digit(radix: {radix = Int32}, c: Char): Option[Int32] =
        import static java.lang.Character.digit(Char, Int32): Int32 \ {};
        match digit(c, radix.radix) {
            case i if i < 0 => None
            case i          => Some(i)
        }

    ///
    /// Returns a character representation of the integer `n` with respect to `radix`. E.g.
    /// `forDigit(radix = 10, 1) => Some('1')`
    /// `forDigit(radix = 16, 11) => Some('a')`
    ///
    /// Returns `None` if `n` is not representable as single character in the radix.
    ///
    pub def forDigit(radix: {radix = Int32}, n: Int32): Option[Char] =
        import static java.lang.Character.forDigit(Int32, Int32): Char \ {};
        match forDigit(n, radix.radix) {
            case c if c == '\u0000' => None
            case c                  => Some(c)
        }

}
