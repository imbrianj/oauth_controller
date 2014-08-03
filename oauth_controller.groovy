/**
 * Copyright (c) 2014 brian@bevey.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

/**
 * Creates OAuth endpoints for devices and modes set up on your SmartThings
 * account.
 *
 * Author: brian@bevey.org
 */

definition(
  name: "OAuth Endpoint",
  namespace: "imbrianj",
  author: "brian@bevey.org",
  description: "OAuth endpoint for Universal Controller",
  iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
  iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png",
  oauth: true
)

preferences {
  section("Allow Endpoint to Control These Things...") {
    input "switches", "capability.switch",         title: "Which Switches?", multiple: true, required: false
    input "locks",    "capability.lock",           title: "Which Locks?",    multiple: true, required: false
    input "contact",  "capability.contactSensor",  title: "Which Contact?",  multiple: true, required: false
    input "moisture", "capability.waterSensor",    title: "Which Moisture?", multiple: true, required: false
    input "motion",   "capability.motionSensor",   title: "Which Motion?",   multiple: true, required: false
    input "presence", "capability.presenceSensor", title: "Which Presence?", multiple: true, required: false
  }

  section("IP:PORT of local endpoint") {
    input "endpoint", "decimal", title: "IP:PORT of local endpoint", required: false
  }
}

  /*********************/
 /* EVENT REGISTERING */
/*********************/
def installed() {
  init()
}

def updated() {
  unsubscribe()
  init()
}

def init() {
  if(endpoint) {
    subscribe(location, "mode",        modeEventFired)
    subscribe(switches, "switch",      eventFired)
    subscribe(locks,    "lock",        eventFired)
    subscribe(temp,     "temperature", eventFired)
    subscribe(contact,  "contact",     eventFired)
    subscribe(moisture, "moisture",    eventFired)
    subscribe(motion,   "motion",      eventFired)
    subscribe(presence, "presence",    eventFired)
  }
}

def modeEventFired(evt) {
  sendUpdate(evt.value.capitalize(), 'Mode')
}

def eventFired(evt) {
  sendUpdate(evt.displayName, evt.value.capitalize())
}

def sendUpdate(name, value) {
  log.warn(name + " is now " + value)

  // Numeric values (such as temp) should be delineated with a dash.
  if(value.isNumber()) {
    value = "-" + value;
  }

  def hubAction = sendHubCommand(new physicalgraph.device.HubAction(
    method: "GET",
    path: "/",
    headers: [HOST:endpoint, REST:true],
    query: ["smartthings":"subdevice-state" + value + "-" + name]
  ))

  if(options) {
    hubAction.options = options
  }

  hubAction
}

  /****************/
 /* API ENDPOINT */
/****************/
mappings {
  path("/switches/:id/:command") {
    action: [GET: "updateSwitch"]
  }

  path("/locks/:id/:command") {
    action: [GET: "updateLock"]
  }

  path("/mode/:mode") {
    action: [GET: "updateMode"]
  }

  path("/list") {
    action: [GET: "listDevices"]
  }
}

def listDevices() {
  printDevices(null, null)
}

def printDevices(device, newValue) {
  def mode = params.mode ? params.mode : location.mode

  return [mode: mode, devices: (settings.switches + settings.locks + settings.contact + settings.moisture + settings.motion + settings.presence).collect{deviceJson(it, device, newValue)}]
}

def deviceJson(it, device, newValue) {
  if (!it) { return [] }

  def values = [:]

  for (a in it.supportedAttributes) {
    if(it == device && (a.name == 'switch' || a.name == 'lock')) {
      values[a.name] = [name  : a.name,
                        value : newValue]
    }

    else {
      values[a.name] = it.currentState(a.name)
    }
  }

  return [label  : it.displayName,
          name   : it.name,
          id     : it.id,
          values : values]
}

def updateSwitch() {
  update(switches, "switch")
}

def updateLock() {
  update(locks, "lock")
}

// Modes
def updateMode() {
  log.debug "Mode change request: params: ${params}"

  setLocationMode(params.mode)

  listDevices()
}

def update(devices, type) {
  def command  = params.command
  def device   = devices.find { it.id == params.id }
  def newValue = ''

  if (command) {
    if (!device) {
      httpError(404, "Device not found")
    }

    else {
      if(command == "toggle") {
        if(type == 'switch') {
          if(device.currentValue(type) == "on") {
            device.off();
            newValue = "off"
          }

          else {
            device.on();
            newValue = "on"
          }
        }

        if(type == 'lock') {
          if(device.currentValue(type) == "locked") {
            device.off();
            newValue = "unlock"
          }

          else {
            device.on();
            newValue = "lock"
          }
        }
      }

      if(!newValue) {
        device."$command"()
        newValue = command
      }
    }
  }

  printDevices(device, newValue)
}
