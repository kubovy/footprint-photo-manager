package com.poterion.footprint.manager.xuggle

import com.poterion.footprint.manager.utils.cifsContext
import com.poterion.footprint.manager.utils.device
import com.xuggle.xuggler.io.IURLProtocolHandler
import com.xuggle.xuggler.io.IURLProtocolHandlerFactory
import jcifs.context.SingletonContext
import java.net.URI

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class SmbFileProtocolHandlerFactory : IURLProtocolHandlerFactory {
	override fun getHandler(protocol: String, url: String, flags: Int): IURLProtocolHandler {
		return SmbFileProtocolHandler(url, URI(url).device?.cifsContext ?: SingletonContext.getInstance())
	}
}