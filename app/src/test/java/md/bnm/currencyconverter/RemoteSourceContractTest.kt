package md.bnm.currencyconverter

import kotlinx.coroutines.runBlocking
import md.bnm.currencyconverter.data.BnmRemoteSource
import md.bnm.currencyconverter.data.BnmXmlParser
import md.bnm.currencyconverter.domain.*
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.math.BigDecimal
import java.time.*

/** The same caller contract is exercised against an in-memory source and the actual HTTP/XML adapter. */
@RunWith(Parameterized::class)
class RemoteSourceContractTest(private val httpAdapter: Boolean) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name="HTTP adapter = {0}") fun cases()=listOf(arrayOf(false),arrayOf(true))
    }
    private val date=LocalDate.of(2026,11,4)
    private val clock=Clock.fixed(Instant.parse("2026-11-04T12:00:00Z"),ZoneOffset.UTC)
    private val rates=mapOf(Currency.MDL to BigDecimal.ONE,Currency.USD to BigDecimal("18"),Currency.EUR to BigDecimal("20"))
    private val xml="""<ValCurs Date="04.11.2026" name="Synthetic HTTP fixture"><Valute><CharCode>USD</CharCode><Nominal>10</Nominal><Value>180</Value></Valute><Valute><CharCode>EUR</CharCode><Nominal>1</Nominal><Value>20</Value></Valute></ValCurs>"""
    private fun source(server: MockWebServer, outcome: RemoteOutcome): RateRemoteSource =
        if(httpAdapter) BnmRemoteSource(OkHttpClient(),BnmXmlParser(),clock,server.url("/rates").toString())
        else object: RateRemoteSource { override suspend fun fetch(date: LocalDate)=outcome }

    @Test fun publishedSourcePreservesDateDecimalUnitsAndTimestamp()=runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody(xml))
            val outcome=source(server,RemoteOutcome.Published(RateSnapshot(date,"Synthetic fixture",rates,clock.instant()))).fetch(date)
            assertTrue(outcome is RemoteOutcome.Published)
            val snapshot=(outcome as RemoteOutcome.Published).snapshot
            assertEquals(date,snapshot.date);assertEquals(clock.instant(),snapshot.fetchedAt)
            assertEquals(0,BigDecimal("18").compareTo(snapshot.ratesMdlPerUnit.getValue(Currency.USD)))
            assertEquals(0,BigDecimal.ONE.compareTo(snapshot.ratesMdlPerUnit.getValue(Currency.MDL)))
            if(httpAdapter) assertTrue(server.takeRequest().path!!.contains("date=04.11.2026"))
        }
    }
    @Test fun absentPublicationIsDistinctFromFailure()=runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(404))
            assertEquals(RemoteOutcome.Unpublished,source(server,RemoteOutcome.Unpublished).fetch(date))
        }
    }
    @Test fun malformedInputYieldsTypedFailureWithoutThrowing()=runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("<ValCurs>broken"))
            assertTrue(source(server,RemoteOutcome.Failure("Synthetic malformed response")).fetch(date) is RemoteOutcome.Failure)
        }
    }
}
