const abs = (n) => (n < 0n ? -n : n);

const gcd = (a, b) => {
    a = abs(a);
    b = abs(b);
    while (b !== 0n) {
        let t = b;
        b = a % b;
        a = t;
    }
    return a;
};

class Fraction {
    constructor(num, den = 1n) {
        this.num = num;
        this.den = den;
        this.simplify();
    }

    simplify() {
        if (this.den === 0n) {
            throw new Error('Zero denominator');
        }
        if (this.den < 0n) {
            this.num = -this.num;
            this.den = -this.den;
        }
        const g = gcd(abs(this.num), this.den);
        this.num /= g;
        this.den /= g;
    }

    static add(a, b) {
        const g = gcd(a.den, b.den);
        const common = (a.den / g) * b.den;
        const num = a.num * (b.den / g) + b.num * (a.den / g);
        return new Fraction(num, common);
    }

    static mul(a, b) {
        const num = a.num * b.num;
        const den = a.den * b.den;
        return new Fraction(num, den);
    }
}

const strToBigInt = (str, base) => {
    let res = 0n;
    str = str.toLowerCase();
    for (let char of str) {
        let digit;
        if (char >= '0' && char <= '9') {
            digit = parseInt(char);
        } else {
            digit = 10 + (char.charCodeAt(0) - 'a'.charCodeAt(0));
        }
        if (digit >= base || digit < 0) {
            throw new Error('Invalid digit');
        }
        res = res * BigInt(base) + BigInt(digit);
    }
    return res;
};

const findSecret = (test) => {
    const keys = test.keys;
    const n = keys.n;
    const k = keys.k;
    let points = [];
    for (let key in test) {
        if (key === 'keys') continue;
        const x = BigInt(key);
        const obj = test[key];
        const base = parseInt(obj.base);
        const value = obj.value;
        const y = strToBigInt(value, base);
        points.push({ x, y });
    }
    points.sort((a, b) => (a.x < b.x ? -1 : a.x > b.x ? 1 : 0));
    const selected = points.slice(0, k);
    const zero = 0n;
    let sum = new Fraction(0n, 1n);
    for (let i = 0; i < k; i++) {
        const xi = selected[i].x;
        const yi = selected[i].y;
        let term_num = 1n;
        let term_den = 1n;
        for (let j = 0; j < k; j++) {
            if (i === j) continue;
            const xj = selected[j].x;
            term_num *= zero - xj;
            term_den *= xi - xj;
        }
        const l = new Fraction(term_num, term_den);
        const term = Fraction.mul(new Fraction(yi, 1n), l);
        sum = Fraction.add(sum, term);
    }
    if (sum.den !== 1n) {
        if (sum.num % sum.den !== 0n) {
            throw new Error('Not an integer');
        }
        return sum.num / sum.den;
    }
    return sum.num;
};

// Test Case 1
const test1 = {
    "keys": {
        "n": 4,
        "k": 3
    },
    "1": {
        "base": "10",
        "value": "4"
    },
    "2": {
        "base": "2",
        "value": "111"
    },
    "3": {
        "base": "10",
        "value": "12"
    },
    "6": {
        "base": "4",
        "value": "213"
    }
};

// Test Case 2
const test2 = {
    "keys": {
        "n": 10,
        "k": 7
    },
    "1": {
        "base": "7",
        "value": "420020006424065463"
    },
    "2": {
        "base": "7",
        "value": "10511630252064643035"
    },
    "3": {
        "base": "2",
        "value": "101010101001100101011100000001000111010010111101100100010"
    },
    "4": {
        "base": "8",
        "value": "31261003022226126015"
    },
    "5": {
        "base": "7",
        "value": "2564201006101516132035"
    },
    "6": {
        "base": "15",
        "value": "a3c97ed550c69484"
    },
    "7": {
        "base": "13",
        "value": "134b08c8739552a734"
    },
    "8": {
        "base": "10",
        "value": "23600283241050447333"
    },
    "9": {
        "base": "9",
        "value": "375870320616068547135"
    },
    "10": {
        "base": "6",
        "value": "30140555423010311322515333"
    }
};

// Output secrets for both test cases
console.log("Test Case 1 Secret:", findSecret(test1).toString());
console.log("Test Case 2 Secret:", findSecret(test2).toString());
