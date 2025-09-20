/**
 * Calculates the magnitude (absolute value) for a BigInt.
 * @param {bigint} num The input BigInt.
 * @returns {bigint} The non-negative value of the input.
 */
const getMagnitude = (num) => (num < 0n ? -num : num);

/**
 * Finds the greatest common factor of two BigInts using the Euclidean algorithm.
 * @param {bigint} num1 The first number.
 * @param {bigint} num2 The second number.
 * @returns {bigint} The greatest common factor.
 */
const findCommonFactor = (num1, num2) => {
    num1 = getMagnitude(num1);
    num2 = getMagnitude(num2);
    while (num2 !== 0n) {
        let temp = num2;
        num2 = num1 % num2;
        num1 = temp;
    }
    return num1;
};

/**
 * Represents a rational number (a fraction) with a numerator and a denominator.
 */
class RationalNumber {
    constructor(numerator, denominator = 1n) {
        this.numerator = numerator;
        this.denominator = denominator;
        this.reduce();
    }

    /**
     * Reduces the fraction to its simplest form and ensures the denominator is positive.
     */
    reduce() {
        if (this.denominator === 0n) {
            throw new Error('Denominator cannot be zero.');
        }
        if (this.denominator < 0n) {
            this.numerator = -this.numerator;
            this.denominator = -this.denominator;
        }
        const commonFactor = findCommonFactor(getMagnitude(this.numerator), this.denominator);
        this.numerator /= commonFactor;
        this.denominator /= commonFactor;
    }

    /**
     * Adds two RationalNumber objects.
     * @param {RationalNumber} r1 The first rational number.
     * @param {RationalNumber} r2 The second rational number.
     * @returns {RationalNumber} A new RationalNumber instance representing the sum.
     */
    static sum(r1, r2) {
        const commonFactor = findCommonFactor(r1.denominator, r2.denominator);
        const commonDenominator = (r1.denominator / commonFactor) * r2.denominator;
        const resultingNumerator = r1.numerator * (r2.denominator / commonFactor) + r2.numerator * (r1.denominator / commonFactor);
        return new RationalNumber(resultingNumerator, commonDenominator);
    }

    /**
     * Multiplies two RationalNumber objects.
     * @param {RationalNumber} r1 The first rational number.
     * @param {RationalNumber} r2 The second rational number.
     * @returns {RationalNumber} A new RationalNumber instance representing the product.
     */
    static product(r1, r2) {
        const newNumerator = r1.numerator * r2.numerator;
        const newDenominator = r1.denominator * r2.denominator;
        return new RationalNumber(newNumerator, newDenominator);
    }
}

/**
 * Parses a string representation of a number in a given base into a BigInt.
 * @param {string} inputString The string to parse.
 * @param {number} radix The base of the number system (e.g., 2, 10, 16).
 * @returns {bigint} The resulting BigInt.
 */
const parseBigIntFromBase = (inputString, radix) => {
    let result = 0n;
    inputString = inputString.toLowerCase();
    for (let character of inputString) {
        let digitValue;
        if (character >= '0' && character <= '9') {
            digitValue = parseInt(character);
        } else {
            digitValue = 10 + (character.charCodeAt(0) - 'a'.charCodeAt(0));
        }
        if (digitValue >= radix || digitValue < 0) {
            throw new Error('Invalid digit for the given radix');
        }
        result = result * BigInt(radix) + BigInt(digitValue);
    }
    return result;
};

/**
 * Reconstructs the original secret value from a set of shares using Lagrange interpolation.
 * @param {object} shareData The object containing the shares and configuration.
 * @returns {bigint} The reconstructed secret value.
 */
const reconstructValue = (shareData) => {
    const config = shareData.keys;
    const requiredShares = config.k;
    let dataPoints = [];

    // Parse the shares into a list of (x, y) coordinates
    for (let pointId in shareData) {
        if (pointId === 'keys') continue;
        const xCoordinate = BigInt(pointId);
        const shareInfo = shareData[pointId];
        const numericBase = parseInt(shareInfo.base);
        const encodedValue = shareInfo.value;
        const yCoordinate = parseBigIntFromBase(encodedValue, numericBase);
        dataPoints.push({ x: xCoordinate, y: yCoordinate });
    }

    // Sort points by x-coordinate to ensure consistent selection
    dataPoints.sort((a, b) => (a.x < b.x ? -1 : a.x > b.x ? 1 : 0));
    const selectedPoints = dataPoints.slice(0, requiredShares);

    const bigIntZero = 0n;
    let interpolatedSum = new RationalNumber(0n, 1n);

    // Perform Lagrange interpolation to find the polynomial's value at x=0
    for (let i = 0; i < requiredShares; i++) {
        const currentX = selectedPoints[i].x;
        const currentY = selectedPoints[i].y;

        let lagrangeNumerator = 1n;
        let lagrangeDenominator = 1n;

        for (let j = 0; j < requiredShares; j++) {
            if (i === j) continue;
            const otherX = selectedPoints[j].x;
            lagrangeNumerator *= bigIntZero - otherX; // (0 - xj)
            lagrangeDenominator *= currentX - otherX; // (xi - xj)
        }

        const lagrangeBasis = new RationalNumber(lagrangeNumerator, lagrangeDenominator);
        const weightedTerm = RationalNumber.product(new RationalNumber(currentY, 1n), lagrangeBasis);
        interpolatedSum = RationalNumber.sum(interpolatedSum, weightedTerm);
    }

    // The final result must be an integer
    if (interpolatedSum.denominator !== 1n) {
        if (interpolatedSum.numerator % interpolatedSum.denominator !== 0n) {
            throw new Error('Result is not a whole number.');
        }
        return interpolatedSum.numerator / interpolatedSum.denominator;
    }
    return interpolatedSum.numerator;
};

// Test Case 1
const dataSet1 = {
    "keys": { "n": 4, "k": 3 },
    "1": { "base": "10", "value": "4" },
    "2": { "base": "2", "value": "111" },
    "3": { "base": "10", "value": "12" },
    "6": { "base": "4", "value": "213" }
};

// Test Case 2
const dataSet2 = {
    "keys": { "n": 10, "k": 7 },
    "1": { "base": "7", "value": "420020006424065463" },
    "2": { "base": "7", "value": "10511630252064643035" },
    "3": { "base": "2", "value": "101010101001100101011100000001000111010010111101100100010" },
    "4": { "base": "8", "value": "31261003022226126015" },
    "5": { "base": "7", "value": "2564201006101516132035" },
    "6": { "base": "15", "value": "a3c97ed550c69484" },
    "7": { "base": "13", "value": "134b08c8739552a734" },
    "8": { "base": "10", "value": "23600283241050447333" },
    "9": { "base": "9", "value": "375870320616068547135" },
    "10": { "base": "6", "value": "30140555423010311322515333" }
};

// Output secrets for both test cases
console.log("Data Set 1 Reconstructed Value:", reconstructValue(dataSet1).toString());
console.log("Data Set 2 Reconstructed Value:", reconstructValue(dataSet2).toString());
