package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.TxnType
import com.example.parser.PdfStatementParser
import com.example.ui.components.CurrencyFormatUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("RupeeFlow", appName)
  }

  @Test
  fun `parses a debit line with amount and balance columns`() {
    val text = "01/08/2026 UPI-SWIGGY-450123456789 450.00 84550.00"
    val result = PdfStatementParser.parseTransactions(text)
    assertEquals(1, result.size)
    assertEquals(450.0, result[0].amount, 0.01)
    assertEquals(TxnType.DEBIT, result[0].type)
    assertEquals(84550.0, result[0].balance)
  }

  @Test
  fun `parses a credit line via keyword when only one amount is present`() {
    val text = "05-Aug-2026 NEFT SALARY CREDIT FROM ACME CORP 85000.00"
    val result = PdfStatementParser.parseTransactions(text)
    assertEquals(1, result.size)
    assertEquals(85000.0, result[0].amount, 0.01)
    assertEquals(TxnType.CREDIT, result[0].type)
  }

  @Test
  fun `test INR currency formatting`() {
    val formatted = CurrencyFormatUtils.formatInr(145230.0)
    assertEquals("₹1,45,230", formatted)
  }
}
