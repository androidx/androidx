/**
 * Supports regular expressions, while falling back gracefully to substring matching.
 */
export function expressionFilter(expression: string) {
  let regExp: RegExp | null = null;
  try {
    regExp = new RegExp(expression, 'g')
  } catch (error) {
    // Invalid regular expression.
    // Falling back to substring matching.
    console.warn(`Invalid regular expression ${expression}. Falling back to substring matching.`)
  }
  if (regExp) {
    return (label: string) => regExp.test(label);
  }
  return (label: string) => label.indexOf(expression) >= 0;
}
