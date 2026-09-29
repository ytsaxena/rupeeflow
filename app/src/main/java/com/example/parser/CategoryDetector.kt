package com.example.parser

import java.util.Locale

/**
 * Best-effort auto-categorization from merchant/narration keywords commonly seen in Indian
 * bank statements and UPI references. Returns null (leaving the transaction uncategorized)
 * rather than a generic "Other" when nothing matches, so the user can still tell the
 * difference between "we guessed wrong" and "we didn't guess at all".
 */
object CategoryDetector {

    private val RULES: List<Pair<List<String>, String>> = listOf(
        listOf("swiggy", "zomato", "mcdonald", "kfc", "burger king", "dominos", "pizza hut", "restaurant", "eatery", "dine") to "Food",
        listOf("starbucks", "cafe coffee day", "ccd", "barista", "chai point", "coffee", "tea stall") to "Beverage",
        listOf("bigbasket", "blinkit", "zepto", "grofers", "dmart", "d-mart", "reliance fresh", "grocery", "supermarket", "nature's basket") to "Groceries",
        listOf("rent", "nobroker", "housing society", "maintenance charge") to "Rent",
        listOf("electricity", "bescom", "tneb", "mseb", "uppcl", "cesc", "airtel", "jio", "vodafone", "broadband", "water bill", "piped gas", "recharge", "dth") to "Bills & Utilities",
        listOf("uber", "ola cabs", "rapido", "irctc", "metro card", "dmrc", "petrol", "diesel", "fuel", "indian oil", "hpcl", "bpcl", "parking", "toll plaza") to "Transport",
        listOf("amazon", "flipkart", "myntra", "ajio", "nykaa", "meesho", "tata cliq") to "Shopping",
        listOf("netflix", "prime video", "hotstar", "spotify", "bookmyshow", "pvr", "inox") to "Entertainment",
        listOf("apollo", "pharmeasy", "1mg", "netmeds", "hospital", "clinic", "pharmacy", "medplus", "diagnostic") to "Health",
        listOf("salary", "payroll", "stipend") to "Salary",
        listOf("sip", "mutual fund", "zerodha", "groww", "upstox", "kite", "nps", "ppf", "fixed deposit", "recurring deposit", "demat", "nse", "bse") to "Investment",
        // Checked before "Loan & EMI" — "premium" contains "emi" as a substring, so an insurance
        // premium payment must be matched here first or it would wrongly fall into Loan & EMI.
        listOf("insurance", "lic premium", "policy premium", "term insurance", "health insurance premium") to "Insurance",
        listOf("emi", "loan emi", "personal loan", "home loan", "car loan", "instalment", "installment", "loan disbursement", "loan repayment") to "Loan & EMI",
        listOf("bank charges", "account maintenance", "amc charges", "bank amc", "sms alert charge", "min bal charge", "minimum balance charge", "chq bounce", "cheque return", "neft charges", "imps charges", "rtgs charges", "atm fee", "od interest", "overdraft interest", "penal interest", "late payment charge", "credit card bill", "cc bill payment", "card annual fee") to "Bank Charges"
    )

    fun detect(description: String): String? {
        val lower = description.lowercase(Locale.ROOT)
        for ((keywords, category) in RULES) {
            if (keywords.any { lower.contains(it) }) return category
        }
        return null
    }
}
