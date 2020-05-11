package no.nav.tag.innsynAareg.service.altinn

import lombok.extern.slf4j.Slf4j
import no.nav.tag.innsynAareg.models.altinn.AltinnException
import no.nav.tag.innsynAareg.models.altinn.Organisasjon
import no.nav.tag.innsynAareg.models.altinn.Role
import no.nav.tag.innsynAareg.service.altinn.AltinnCacheConfig.Companion.ALTINN_CACHE
import no.nav.tag.innsynAareg.service.altinn.AltinnCacheConfig.Companion.ALTINN_TJENESTE_CACHE
import no.nav.tag.innsynAareg.utils.TokenUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
import java.util.*
import kotlin.collections.HashSet
import kotlin.collections.List
import kotlin.collections.MutableSet

@Slf4j
@Component
class AltinnService @Autowired constructor(altinnConfig: AltinnConfig, private val restTemplate: RestTemplate, tokenUtils: TokenUtils) {
    private val headerEntity: HttpEntity<HttpHeaders?>
    private val altinnUrl: String
    private val altinnProxyUrl: String
    private val tokenUtils: TokenUtils

    @Cacheable(ALTINN_CACHE)
    fun hentOrganisasjoner(fnr: String): List<Organisasjon> {
        val query = "&\$filter=Type+ne+'Person'+and+Status+eq+'Active'"
        //AltinnService.log.info("Henter organisasjoner fra Altinn")
        return hentReporteesFraAltinn(query, fnr)
    }

    fun hentRoller(fnr: String, orgnr: String): List<Role> {
        val query = "&subject=$fnr&reportee=$orgnr"
        val url = altinnUrl + "authorization/roles?ForceEIAuthentication" + query
        //AltinnService.log.info("Henter roller fra Altinn")
        //return getFromAltinn<T>(object : ParameterizedTypeReference<List<Role?>?>() {}, url, ALTINN_ROLE_PAGE_SIZE, headerEntity)
    }

    @Cacheable(ALTINN_TJENESTE_CACHE)
    fun hentOrganisasjonerBasertPaRettigheter(fnr: String, serviceKode: String, serviceEdition: String): List<Organisasjon> {
        val query = ("&serviceCode=" + serviceKode
                + "&serviceEdition=" + serviceEdition)
        //AltinnService.log.info("Henter rettigheter fra Altinn")
        return hentReporteesFraAltinn(query, fnr)
    }

    private fun hentReporteesFraAltinn(query: String, fnr: String): List<Organisasjon> {
        var query = query
        val baseUrl: String
        val headers: HttpEntity<HttpHeaders?>
        baseUrl = altinnUrl
        headers = headerEntity
        query += "&subject=$fnr"
        val url = baseUrl + "reportees/?ForceEIAuthentication" + query
        return getFromAltinn<Organisasjon>(object : emptyArray<T>() {}, url, ALTINN_ORG_PAGE_SIZE, headers)
    }

    fun <T> getFromAltinn(typeReference: ParameterizedTypeReference<List<T>>, url: String, pageSize: Int, headers: HttpEntity<HttpHeaders?>?): List<T> {
        val response: MutableSet<T> = HashSet()
        var pageNumber = 0
        var hasMore = true
        while (hasMore) {
            pageNumber++
            hasMore = try {
                val urlWithPagesizeAndOffset = url + "&\$top=" + pageSize + "&\$skip=" + (pageNumber - 1) * pageSize
                val respons  = restTemplate.exchange(urlWithPagesizeAndOffset, HttpMethod.GET, headers, Array<Organisasjon>::class)
                response.addAll(exchange.body)
                exchange.size >= pageSize
            } catch (exception: RestClientException) {
                //AltinnService.log.error("Feil fra Altinn med spørring: " + url + " Exception: " + exception.message)
                throw AltinnException("Feil fra Altinn", exception)
            }
        }
        return ArrayList(response)
    }

    private val authHeadersForInnloggetBruker: HttpEntity<HttpHeaders?>
        private get() {
            val headers = HttpHeaders()
            headers.setBearerAuth(tokenUtils.tokenForInnloggetBruker)
            return HttpEntity(headers)
        }

    companion object {
        private const val ALTINN_ORG_PAGE_SIZE = 500
        private const val ALTINN_ROLE_PAGE_SIZE = 50
    }

    init {
        altinnUrl = altinnConfig.altinnurl!!
        altinnProxyUrl = altinnConfig.proxyUrl!!
        this.tokenUtils = tokenUtils
        val headers = HttpHeaders()
        headers["X-NAV-APIKEY"] = altinnConfig.APIGwHeader
        headers["APIKEY"] = altinnConfig.altinnHeader
        headerEntity = HttpEntity(headers)
    }
}