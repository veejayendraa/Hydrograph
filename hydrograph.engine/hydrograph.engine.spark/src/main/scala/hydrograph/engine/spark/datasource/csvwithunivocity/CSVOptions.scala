/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package hydrograph.engine.spark.datasource.csvwithunivocity

import java.nio.charset.StandardCharsets
import java.util.{Locale, TimeZone}

import hydrograph.engine.spark.datasource.utils.CompressionCodecs
import org.apache.commons.lang3.time.FastDateFormat
import org.slf4j.{Logger, LoggerFactory}
/**
  * The Class CSVOptions.
  *
  * @author Bitwise
  *
  */
class CSVOptions(@transient private val parameters: Map[String, String])
  extends Serializable {
  private val LOG: Logger = LoggerFactory.getLogger(classOf[CSVOptions])
  private def getChar(paramName: String, default: Char): Char = {
    val paramValue = parameters.get(paramName)
    paramValue match {
      case None => default
      case Some(null) => default
      case Some(value) if value.length == 0 => '\u0000'
      case Some(value) if value.length == 1 => value.charAt(0)
      case _ => throw new RuntimeException(s"$paramName cannot be more than one character")
    }
  }

  private def getInt(paramName: String, default: Int): Int = {
    val paramValue = parameters.get(paramName)
    paramValue match {
      case None => default
      case Some(null) => default
      case Some(value) => try {
        value.toInt
      } catch {
        case e: NumberFormatException =>
          throw new RuntimeException(s"$paramName should be an integer. Found $value",e)
      }
    }
  }

  private def getBool(paramName: String, default: Boolean = false): Boolean = {
    val param = parameters.getOrElse(paramName, default.toString)
    if (param == null) {
      default
    } else if (param.toLowerCase == "true") {
      true
    } else if (param.toLowerCase == "false") {
      false
    } else {
      throw new Exception(s"$paramName flag can be true or false")
    }
  }

  val delimiter = CSVTypeCast.toChar(
    parameters.getOrElse("sep", parameters.getOrElse("delimiter", ",")))
  private val parseMode = parameters.getOrElse("mode", "PERMISSIVE")
  val charset = parameters.getOrElse("encoding",
    parameters.getOrElse("charset", StandardCharsets.UTF_8.name()))

  val quote = getChar("quote", '\"')
  val escape = getChar("escape", '\\')
  val comment = getChar("comment", '\u0000')

  val headerFlag = getBool("header")
  val inferSchemaFlag = getBool("inferSchema")
  val ignoreLeadingWhiteSpaceFlag = getBool("ignoreLeadingWhiteSpace")
  val ignoreTrailingWhiteSpaceFlag = getBool("ignoreTrailingWhiteSpace")

  // Parse mode flags
  if (!ParseModes.isValidMode(parseMode)) {
    LOG.warn(s"$parseMode is not a valid parse mode. Using ${ParseModes.DEFAULT}.")
  }

  val failFast = ParseModes.isFailFastMode(parseMode)
  val dropMalformed = ParseModes.isDropMalformedMode(parseMode)
  val permissive = ParseModes.isPermissiveMode(parseMode)

  val nullValue = parameters.getOrElse("nullValue", "")

  val nanValue = parameters.getOrElse("nanValue", "NaN")

  val positiveInf = parameters.getOrElse("positiveInf", "Inf")
  val negativeInf = parameters.getOrElse("negativeInf", "-Inf")


  val compressionCodec: Option[String] = {
    val name = parameters.get("compression").orElse(parameters.get("codec"))
    if (name.nonEmpty && name.get != null){
      name.map(CompressionCodecs.getCodecClassName)
    } else {
      null
    }

  }

  private def getDateFormats(dateFormats: List[String]): List[FastDateFormat] = dateFormats.map { e =>
    if (e.equals("null")) {
      null
    } else {
      fastDateFormat(e)
    }
  }
  private def fastDateFormat(dateFormat: String): FastDateFormat = if (!dateFormat.equalsIgnoreCase("null")) {
    val date = FastDateFormat.getInstance(dateFormat,TimeZone.getDefault,Locale.getDefault)
    //    val date = new FastDateFormat(dateFormat, Locale.getDefault)
    //    date.setLenient(false)
    //    date.setTimeZone(TimeZone.getDefault)
    date
  } else null


  // Uses `FastDateFormat` which can be direct replacement for `SimpleDateFormat` and thread-safe.
//  val dateFormat: FastDateFormat =
//    FastDateFormat.getInstance(parameters.getOrElse("dateFormat", "yyyy-MM-dd"))


  val dateFormat: List[FastDateFormat] =  getDateFormats(parameters.getOrElse("dateFormats", "null").split("\t").toList)

 /* val timestampFormat: FastDateFormat =
    FastDateFormat.getInstance(
      parameters.getOrElse("timestampFormat", "yyyy-MM-dd'T'HH:mm:ss.SSSZZ"))*/

  val maxColumns = getInt("maxColumns", 20480)

  val maxCharsPerColumn = getInt("maxCharsPerColumn", 1000000)

  val escapeQuotes = getBool("escapeQuotes", true)

  val maxMalformedLogPerPartition = getInt("maxMalformedLogPerPartition", 10)

  val quoteAll = getBool("quoteAll", false)

  val inputBufferSize = 128

  val isCommentSet = this.comment != '\u0000'

  val rowSeparator = "\n"
}

object CSVOptions {

  def apply(): CSVOptions = new CSVOptions(Map.empty)

  def apply(paramName: String, paramValue: String): CSVOptions = {
    new CSVOptions(Map(paramName -> paramValue))
  }
}
