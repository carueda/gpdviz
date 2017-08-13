package gpdviz

import gpdviz.config.cfg

object ApiImpl extends Api {

  def clientConfig(): ClientConfig = ClientConfig(
    serverName = cfg.serverName,
    pusher = cfg.pusher.map(p ⇒ ClientPusherConfig(p.key))
  )

}

