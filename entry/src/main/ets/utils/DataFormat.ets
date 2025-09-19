export class DataFormat {
  dataFormat(value: Date) {
    const year = value.getFullYear()
    const month = value.getMonth()
    const date = value.getDate()

    return `${year}-${month}-${date}`;
  }
}


export default  new DataFormat()