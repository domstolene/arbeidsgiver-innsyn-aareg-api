package no.nav.tag.innsynAareg.controller
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tag.innsynAareg.utils.ISSUER
import no.nav.tag.innsynAareg.utils.LEVEL
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController

@RestController
@ProtectedWithClaims(issuer=ISSUER , claimMap= [LEVEL])
class InnloggingsController {
    @GetMapping(value = ["/innlogget"])
    @ResponseBody
    fun erInnlogget(): String? {
        return "ok"
    }
}