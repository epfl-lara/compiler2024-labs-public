// NodeJS and Browser compatible
const isNode = typeof window === 'undefined';

const settings = {
  'memory': 100
}

const state = {
  charDecoder: settings.char != null ? new TextDecoder(settings.char) : null,
  charEncoder: settings.char != null ? new TextEncoder(settings.char) : null
}

// These functions are normally type-independant.
// Supposed to be called from WASM.
const api = {
  // Prints the given arguments (WASM limits it to 1 int/float argument)
  'print': (str) => log(str),
  // Prints the character represented by the given char code (JS dependant)
  'print-char': c => {
    log(String.fromCharCode(c))
  },
  'mem': new WebAssembly.Memory({ initial: settings.memory }),
  'show-memory': idx => log(`Heap[${idx}] = ${new Uint32Array(api.mem.buffer)[idx]}`),

  // Reading a character
  'read-char': () => {
    if (isNode)
      return getChar()
    else
      console.error('read-char not implemented in browser')
      return -1
  }
}

const run = async file => {
  let inst;
  try {
    inst = await WebAssembly.instantiate(file, { api })
  } catch(err) {
    console.error(`Error while instantiating WebAssembly: ${err}`)
    return -1;
  }

  if (!inst.instance.exports.main) {
    console.error('No main function found in the WebAssembly module.')
    return -1;
  }

  if (isNode)
    if (process.argv.indexOf('wait-for-debugger') > -1) {
      console.log('Seems like you want to debug this. Attach your debugger. I am waiting (^-^)/')
      const inspector = require('inspector')
      inspector.open(undefined, undefined, true)
    }

  // Run the main function
  debugger;
  return inst.instance.exports.main() // This function calls the main function in the WASM module
}

const getChar = () => {
  let buffer = Buffer.alloc(1)
  fs.readSync(0, buffer, 0, 1)
  return buffer.toString('utf8')
}

const log = (s) => {
  console.log(s)
  if (!isNode) {
    const log = document.getElementById('log')
    log.innerHTML += s + '<br>'
  }
}

if (isNode) {
  exports.run = run;
} else {
  window.run = run;
}