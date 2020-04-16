package no.nav.tag.innsynAareg.service.pdl

import no.nav.tag.innsynAareg.models.pdlPerson.Data
import no.nav.tag.innsynAareg.models.pdlPerson.HentPerson
import no.nav.tag.innsynAareg.models.pdlPerson.Navn
import no.nav.tag.innsynAareg.models.pdlPerson.Error
import no.nav.tag.innsynAareg.models.pdlPerson.PdlRespons
import no.nav.tag.innsynAareg.service.sts.STSClient
import no.nav.tag.innsynAareg.service.sts.STStoken
import no.nav.tag.innsynAareg.utils.GraphQlUtils

import org.junit.Assert
import org.junit.Before

import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpEntity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import java.util.concurrent.ExecutionException


@SpringBootTest
@RunWith(SpringRunner::class)
@ActiveProfiles("local")
@TestPropertySource(properties = ["mock.port=8082"])
class PdlServiceTest {
    var respons: PdlRespons? = null
    @Mock
    private val restTemplate: RestTemplate? = null
    @InjectMocks
    private val pdlService: PdlService? = null
    @Mock
    var stsClient: STSClient? = null
    @Mock
    private val graphQlUtils: GraphQlUtils? = null

    @Before
    fun setUp() {
        Mockito.`when`(stsClient?.token).thenReturn(STStoken())
        respons = PdlRespons()
        lagPdlObjekt()
    }

    fun lagPdlObjekt() {
        respons?.data = Data()
        respons?.data?.hentPerson = HentPerson()
        val testNavn = Navn()
        testNavn.fornavn = "Ole"
        testNavn.etternavn = "Dole"
        respons?.data?.hentPerson?.navn = arrayOf(testNavn);
    }

    @Test
    @Throws(ExecutionException::class, InterruptedException::class)
    fun hentNavnMedFnr_skal_hente_sts_token_og_returnere_navn_på_person() {
        val navn = "Ole Dole"
        Mockito.`when`(restTemplate?.postForObject(ArgumentMatchers.eq(PDL_URL), ArgumentMatchers.any(HttpEntity::class.java), ArgumentMatchers.eq(PdlRespons::class.java)))
                .thenReturn(respons)
        Assert.assertEquals(navn, pdlService!!.hentNavnMedFnr(FNR))
        Mockito.verify<Any?>(stsClient?.token?.access_token)
    }

    @Test
    @Throws(ExecutionException::class, InterruptedException::class)
    fun hentNavnMedFnr_skal_hente_sts_token_og_returnere_ikke_funnet_person() {
        val tomRespons = PdlRespons()
        val ingenPersonError = Error("Fant ikke Person");
        tomRespons.data = Data()
        tomRespons.data?.hentPerson = null
        tomRespons.errors = ArrayList<Error>()
        tomRespons.errors?.add(ingenPersonError)
        Mockito.`when`(restTemplate?.postForObject(ArgumentMatchers.eq(PDL_URL), ArgumentMatchers.any(HttpEntity::class.java), ArgumentMatchers.eq(PdlRespons::class.java)))
                .thenReturn(tomRespons)
        Assert.assertEquals("Kunne ikke hente navn", pdlService!!.hentNavnMedFnr(FNR))
        Mockito.verify<Any?>(stsClient?.token?.access_token)
    }

    @Test
    @Throws(ExecutionException::class, InterruptedException::class)
    fun hentNavnMedFnr_skal_hente_sts_token_og_returnere_ikke_funnet_person_v_helt_tomPdlRespons() {
        val tomRespons = PdlRespons()
        Mockito.`when`(restTemplate?.postForObject(ArgumentMatchers.eq(PDL_URL), ArgumentMatchers.any(HttpEntity::class.java), ArgumentMatchers.eq(PdlRespons::class.java)))
                .thenReturn(tomRespons)
        Assert.assertEquals("Kunne ikke hente navn", pdlService!!.hentNavnMedFnr(FNR))
        Mockito.verify<Any?>(stsClient?.token?.access_token)
    }

    @Test
    @Throws(ExecutionException::class, InterruptedException::class)
    fun hentNavnMedFnr_skal_hente_sts_token_fange_opp_feil() {
        Mockito.`when`(restTemplate?.postForObject(ArgumentMatchers.eq(PDL_URL), ArgumentMatchers.any(HttpEntity::class.java), ArgumentMatchers.eq(PdlRespons::class.java))).thenThrow(RestClientException("401"))
        Assert.assertEquals("Kunne ikke hente navn", pdlService!!.hentNavnMedFnr(FNR))
        Mockito.verify<Any?>(stsClient?.token?.access_token)
    }

    companion object {
        private const val FNR = "123"
        private const val PDL_URL = "http://test"
    }
}