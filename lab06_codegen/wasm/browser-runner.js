(async function() {
  const runBtn = document.getElementById('run')
  const file = document.getElementById('file')

  runBtn.addEventListener('click', async () => {
    let buffer

    if (file.files.length > 0) {
      console.log(`File given (${file.files[0].name}). Reading it… `)
      buffer = await file.files[0].arrayBuffer()
    } else {
      console.error('No file given')
      return
    }

    const inst = await WebAssembly.instantiate(buffer, { api })
    inst.instance.exports.main()
  })
})()