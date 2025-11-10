package test

import goowee.elements.ElementsController
import goowee.security.SecurityService

class KeyPressController implements ElementsController {

    SecurityService securityService

    def onKeyPress() {
        String externalId = keyPressed
        def user = securityService.getUserByExternalId(externalId)
        if (user) {
            display controller: 'authentication', action: 'logout'
            return
        }

        display
    }
}
