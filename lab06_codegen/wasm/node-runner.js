const { run } = require('./run');

(async () => {
  if (process.argv.length < 3) {
    console.error(`Usage: ${process.argv[0]} ${process.argv[1]} <wasm-file>`)
    process.exit(-1)
  }

  // Read the wasm file
  const fs = require('fs')
  try {
    fs.accessSync(process.argv[2], fs.constants.R_OK)
  } catch (err) {
    console.error(`File ${process.argv[2]} not found`)
    process.exit(-1)
  }
  const buf = fs.readFileSync(process.argv[2])
  // if file does not exist
  if (!buf) {
    console.error(`File ${process.argv[2]} not found`)
    process.exit(-1)
  }

  console.log('WASM returned: ' + await run(buf))
})()