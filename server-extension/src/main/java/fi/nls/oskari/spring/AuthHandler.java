package fi.nls.oskari.spring;

import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import org.oskari.spring.extension.OskariParam;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Profile("preauth")
@Controller
@RequestMapping("/auth")
public class AuthHandler {

    private final static Logger LOG = LogFactory.getLogger(AuthHandler.class);

    @RequestMapping
    public ModelAndView index(@OskariParam ActionParameters params) throws Exception {
        LOG.info("User logged in:", params.getRequest().getHeader("auth-email"));
        if(params.getResponse().isCommitted()) {
            // to prevent errors in log -> request has already been handled
            return null;
        }
        return new ModelAndView("redirect:/");
    }

}